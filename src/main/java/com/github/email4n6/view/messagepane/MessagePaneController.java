/*
 * This file is part of Email4n6.
 * Copyright (C) 2018  Marten4n6
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.email4n6.view.messagepane;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import javax.swing.SwingUtilities;

import com.github.email4n6.model.Version;
import com.github.email4n6.model.message.AttachmentRow;
import com.github.email4n6.model.message.MessageRow;
import com.github.email4n6.model.message.MessageValue;
import com.github.email4n6.model.message.factory.MessageFactory;
import com.github.email4n6.utils.PathUtils;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.web.WebView;
import lombok.extern.slf4j.Slf4j;

/**
 * This class controls the message pane.
 *
 * @author Marten4n6
 */
@Slf4j
public class MessagePaneController {

    private MessagePane messagePane;
    private MessageFactory messageFactory;

    public MessagePaneController(MessagePane messagePane, MessageFactory messageFactory) {
        this.messagePane = messagePane;
        this.messageFactory = messageFactory;

        // Catch events fired by the message pane.
        messagePane.setOnMessageSelectionChange(new MessageSelectionChangeListener());
        messagePane.setOnOpenAttachment(new OpenAttachmentListener());
        messagePane.getBodyView().getEngine().getLoadWorker().stateProperty().addListener(new BodyEngineListener(messagePane.getBodyView()));
        messagePane.getBodyView().setOnMouseClicked(new BodyClickListener(messagePane.getBodyView()));
        messagePane.getHeadersView().getEngine().getLoadWorker().stateProperty().addListener(new BodyEngineListener(messagePane.getHeadersView()));
        messagePane.getHeadersView().setOnMouseClicked(new BodyClickListener(messagePane.getHeadersView()));
        messagePane.getTable().setContextMenu(new DefaultContextMenu(messagePane, messageFactory));
    }

    /**
     * Handles selection change events.
     */
    class MessageSelectionChangeListener implements ChangeListener<MessageRow> {

        @Override
        public void changed(ObservableValue<? extends MessageRow> observable, MessageRow oldValue, MessageRow newValue) {
            MessageRow selectedMessage = messagePane.getTable().getSelectionModel().getSelectedItem();

            if (selectedMessage != null) {
                Platform.runLater(() -> {
                    MessageValue messageValue = messageFactory.getMessageValue(selectedMessage.getId());

                    if (messageValue != null) {
                        messagePane.getBodyView().getEngine().loadContent(messageValue.getBody(), "text/html");
                        messagePane.getHeadersView().getEngine().loadContent(messageValue.getHeaders(), "text/html");

                        messagePane.getAttachmentsTable().getItems().clear();
                        if (messageValue.getAttachments() != null) {
                            messagePane.getAttachmentsTable().getItems().addAll(messageValue.getAttachments());
                        }
                    }
                });
            }
        }
    }

    /**
     * Handles the body engine's events.
     */
    class BodyEngineListener implements ChangeListener<Worker.State> {

        private WebView webView;

        BodyEngineListener(WebView webView) {
            this.webView = webView;
        }

        @Override
        public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
            if (!webView.getEngine().getLocation().isEmpty()) {
                // Cancel clicking on links which would most likely be a bad idea.
                Platform.runLater(() -> {
                    log.warn("Canceling request to load: {}", webView.getEngine().getLocation());

                    messagePane.getBodyView().getEngine().getLoadWorker().cancel();
                });
            }

            if (newValue == Worker.State.SUCCEEDED) {
                try {
                    // Page is done loading, add support for highlighting.
                    InputStream inputStream = this.getClass().getResourceAsStream("/js/mark.min.js");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    StringBuilder markScript = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        markScript.append(line);
                    }

                    webView.getEngine().executeScript(markScript.toString());
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * Handles the body click events.
     */
    class BodyClickListener implements EventHandler<MouseEvent> {

        private WebView webView;
        private ContextMenu contextMenu;

        BodyClickListener(WebView webView) {
            this.contextMenu = getMenu();
            this.webView = webView;
        }

        @Override
        public void handle(MouseEvent event) {
            // Custom context menu
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(webView, event.getScreenX(), event.getScreenY());
            } else {
                contextMenu.hide();
            }
        }

        private ContextMenu getMenu() {
            ContextMenu menu = new ContextMenu();

            MenuItem searchItem = new MenuItem("Find");
            KeyCodeCombination searchCombination = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);

            searchItem.setAccelerator(searchCombination);

            messagePane.getBodyView().addEventHandler(KeyEvent.KEY_RELEASED, (event) -> {
                // Key combinations
                if (searchCombination.match(event)) {
                    searchItem.fire();
                }
            });
            searchItem.setOnAction((event) -> {
                // Search menu item
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
                dialog.setHeaderText("Find");

                dialog.showAndWait().ifPresent(result -> {
                    webView.getEngine().executeScript(
                            "var instance = new Mark(document.querySelector(\"body\"));" +
                                    "instance.unmark();" +
                                    "instance.mark(\"" + result + "\", {" +
                                    "    accuracy: \"partially\"," +
                                    "    separateWordSearch: true" +
                                    "});"
                    );
                });
            });

            menu.getItems().add(searchItem);
            return menu;
        }
    }

    /**
     * Handles the open attachment event.
     */
    class OpenAttachmentListener implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent event) {
            Optional<ButtonType> confirmation = messagePane.displayConfirmation("Are you sure you want to open this attachment?");

            if (confirmation.isPresent() && confirmation.get() == ButtonType.YES) {
                AttachmentRow selectedRow = messagePane.getAttachmentsTable().getSelectionModel().getSelectedItem();

                if (selectedRow != null) {
                    File outputFile = new File(PathUtils.getTempPath() + File.separator + selectedRow.getAttachmentName());

                    try {
                        outputFile.deleteOnExit();
                        Files.copy(selectedRow.getInputStream(), outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        SwingUtilities.invokeLater(() -> {
                            try {
                                Desktop.getDesktop().open(outputFile);
                            } catch (IOException ex) {
                                log.error(ex.getMessage(), ex);
                                messagePane.displayError("Failed to open attachment: " + ex.getMessage());
                            }
                        });

                        selectedRow.getInputStream().reset(); // Important!
                    } catch (IOException ex) {
                        log.error(ex.getMessage(), ex);
                        messagePane.displayError("Failed to copy attachment: " + ex.getMessage());
                    }
                }
            }
        }
    }
}
