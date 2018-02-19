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

import com.github.email4n6.model.casedao.Case;
import com.github.email4n6.model.Settings;
import com.github.email4n6.message.AttachmentRow;
import com.github.email4n6.message.MessageRow;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * Simple panel for the message table and tabbed pane, follows the MVC pattern.
 *
 * @author Marten4n6
 */
@Slf4j
public class MessagePane {

    private @Getter SplitPane pane;
    private @Getter Case currentCase;
    private @Getter TableView<MessageRow> table;

    private SimpleDateFormat DATE_FORMAT;

    private @Getter WebView bodyView;
    private @Getter WebView headersView;
    private @Getter TableView<AttachmentRow> attachmentsTable;

    private @Setter ChangeListener<MessageRow> onMessageSelectionChange;
    private @Setter EventHandler<ActionEvent> onOpenAttachment;

    public MessagePane(Case currentCase) {
        this.currentCase = currentCase;
        this.DATE_FORMAT = new SimpleDateFormat(Settings.get(currentCase.getName(), "DateFormat"));

        // Split Pane
        pane = new SplitPane();

        pane.setOrientation(Orientation.VERTICAL);
        pane.setMaxWidth(Double.MAX_VALUE);
        pane.setMaxHeight(Double.MAX_VALUE);

        pane.getItems().addAll(createTable(), createTabPane());
    }

    /**
     * @return The table which contains all messages.
     */
    private TableView<MessageRow> createTable() {
        table = new TableView<>();

        TableColumn<MessageRow, String> columnFrom = new TableColumn<>("From");
        TableColumn<MessageRow, String> columnTo = new TableColumn<>("To");
        TableColumn<MessageRow, String> columnSubject = new TableColumn<>("Subject");
        TableColumn<MessageRow, Date> columnReceived = new TableColumn<>("Received");
        TableColumn<MessageRow, String> columnSize = new TableColumn<>("Size");
        TableColumn<MessageRow, String> columnCC = new TableColumn<>("CC");
        TableColumn<MessageRow, String> columnTag = new TableColumn<>("Tag");
        TableColumn<MessageRow, Boolean> columnBookmark = new TableColumn<>("Bookmark");

        columnFrom.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getFrom()));
        columnTo.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getTo()));
        columnSubject.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getSubject()));
        columnReceived.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getReceivedDate()));
        columnSize.setCellValueFactory(param -> new SimpleStringProperty(humanReadableByteCount(param.getValue().getSize())));
        columnCC.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getCc()));
        columnTag.setCellValueFactory(param -> param.getValue().getTag());
        columnBookmark.setCellValueFactory(param -> param.getValue().getBookmarked());

        columnReceived.setCellFactory(column -> new TableCell<MessageRow, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(DATE_FORMAT.format(item));
                }
            }
        });
        columnReceived.setComparator((Date date1, Date date2) -> {
            // Sort date from new to old.
            if (date1 == null && date2 == null) {
                return 0;
            } else if (date1 == null) {
                return 1;
            } else if (date2 == null) {
                return -1;
            }
            return date2.compareTo(date1);
        });
        columnBookmark.setCellFactory((TableColumn<MessageRow, Boolean> param) -> {
            CheckBoxTableCell<MessageRow, Boolean> tableCell = new CheckBoxTableCell<>();

            // Callback to the bookmarked boolean property
            tableCell.setSelectedStateCallback((itemIndex) -> table.getItems().get(itemIndex).getBookmarked());
            return tableCell;
        });

        table.getColumns().add(columnFrom);
        table.getColumns().add(columnTo);
        table.getColumns().add(columnSubject);
        table.getColumns().add(columnReceived);
        table.getColumns().add(columnSize);
        table.getColumns().add(columnCC);
        table.getColumns().add(columnTag);
        table.getColumns().add(columnBookmark);

        table.setPlaceholder(new Label("No messages added."));
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Listeners
        table.getSelectionModel().selectedItemProperty().addListener((ObservableValue<? extends MessageRow> observable, MessageRow oldValue, MessageRow newValue) -> {
            onMessageSelectionChange.changed(observable, oldValue, newValue);
        });

        return table;
    }

    /**
     * @return A human readable byte size.
     */
    private String humanReadableByteCount(long bytes) {
        boolean si = true;
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * @return The tab pane which contains the body, headers and attachments.
     */
    private TabPane createTabPane() {
        TabPane tabPane = new TabPane();

        tabPane.getTabs().addAll(
                createBodyTab(),
                createHeadersTab(),
                createAttachmentsTab()
        );
        return tabPane;
    }

    /**
     * Creates the body tab.
     */
    private Tab createBodyTab() {
        Tab messageTab = new Tab();
        BorderPane layout = new BorderPane();
        bodyView = new WebView();

        messageTab.setClosable(false);
        messageTab.setText("Body");
        messageTab.setContent(layout);

        bodyView.setContextMenuEnabled(false); // We have our own.

        layout.setCenter(bodyView);
        return messageTab;
    }

    /**
     * Creates the headers tab.
     */
    private Tab createHeadersTab() {
        Tab headersTab = new Tab();
        BorderPane layout = new BorderPane();
        headersView = new WebView();

        headersTab.setClosable(false);
        headersTab.setText("Headers");
        headersTab.setContent(layout);

        layout.setCenter(headersView);
        return headersTab;
    }

    /**
     * Creates the attachments tab.
     */
    private Tab createAttachmentsTab() {
        Tab attachmentsTab = new Tab();
        BorderPane layout = new BorderPane();
        attachmentsTable = new TableView<>();

        TableColumn<AttachmentRow, String> columnName = new TableColumn<>("Name");
        TableColumn<AttachmentRow, String> columnSize = new TableColumn<>("Size");
        TableColumn<AttachmentRow, Date> columnLastModification = new TableColumn<>("Last Modification");

        columnName.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getAttachmentName()));
        columnSize.setCellValueFactory(param -> new SimpleStringProperty(humanReadableByteCount(param.getValue().getSize())));
        columnLastModification.setCellValueFactory(param -> new SimpleObjectProperty<>(param.getValue().getLastModificationTime()));

        columnLastModification.setCellFactory(column -> new TableCell<AttachmentRow, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(DATE_FORMAT.format(item));
                }
            }
        });

        attachmentsTable.setPlaceholder(new Label("No attachments added."));
        attachmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        attachmentsTable.getColumns().add(columnName);
        attachmentsTable.getColumns().add(columnSize);
        attachmentsTable.getColumns().add(columnLastModification);
        attachmentsTable.addEventHandler(KeyEvent.KEY_PRESSED, (event) -> {
            if (event.getCode().equals(KeyCode.ENTER)) {
                onOpenAttachment.handle(new ActionEvent());
            }
        });

        // Context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem openItem = new MenuItem("Open");

        contextMenu.getItems().add(openItem);

        attachmentsTable.setOnMouseClicked((event) -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(attachmentsTable, event.getScreenX(), event.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
        openItem.setOnAction((event) -> {
            onOpenAttachment.handle(event);
        });

        attachmentsTab.setClosable(false);
        IntegerBinding sizeBinding = Bindings.size(attachmentsTable.getItems());
        StringExpression stringExpression = Bindings.concat("Attachments (", sizeBinding.asString(), ")");
        attachmentsTab.textProperty().bind(stringExpression);
        attachmentsTab.setContent(layout);

        layout.setCenter(attachmentsTable);
        return attachmentsTab;
    }

    /**
     * Shows a confirmation alert to the user.
     *
     * @return The (optional) ButtonType clicked.
     */
    public Optional<ButtonType> displayConfirmation(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);

        alert.setTitle("Email4n6");
        alert.setHeaderText(null);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.initOwner(table.getScene().getWindow());

        return alert.showAndWait();
    }

    /**
     * Shows an error alert to the user.
     */
    public void displayError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);

        alert.setTitle("Email4n6");
        alert.setHeaderText(null);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.initOwner(table.getScene().getWindow());

        alert.show();
    }

    public void setLoading(boolean loading) {
        if (loading) {
            table.setCursor(Cursor.WAIT);
        } else {
            table.setCursor(Cursor.DEFAULT);
        }
    }

    /**
     * Clears the message body, headers and attachments.
     */
    public void clear() {
        bodyView.getEngine().loadContent("", "text/html");
        headersView.getEngine().loadContent("", "text/html");
        attachmentsTable.getItems().clear();
    }
}
