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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.github.email4n6.model.message.AttachmentRow;
import com.github.email4n6.model.message.MessageRow;
import com.github.email4n6.model.message.MessageValue;
import com.github.email4n6.model.message.factory.MessageFactory;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * The default context menu used by the message pane.
 *
 * @author Marten4n6
 */
@Slf4j
public class DefaultContextMenu extends ContextMenu {

    private @Setter ShowInTreeEvent onShowInTree;

    /**
     * Initializes the default context menu.
     */
    public DefaultContextMenu(MessagePane messagePane, MessageFactory messageFactory) {
        Menu menuBookmark = new Menu("Bookmark");
        Menu menuTag = new Menu("Tag");
        Menu menuExport = new Menu("Export");
        Menu menuShowIn = new Menu("Show In");

        menuBookmark.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/star.png"))));
        menuTag.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/tag.png"))));
        menuExport.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/export.png"))));
        menuShowIn.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/eye.png"))));

        // Bookmark
        MenuItem bookmarksAddSelected = new MenuItem("Add selected");
        MenuItem bookmarksRemoveSelected = new MenuItem("Remove selected");

        // Tag
        MenuItem tagAddSelected = new MenuItem("Add selected");
        MenuItem tagRemoveSelected = new MenuItem("Remove selected");

        // Export
        MenuItem exportAttachments = new MenuItem("Attachments");

        // Show In
        MenuItem showInTree = new MenuItem("Tree");

        // Add
        menuBookmark.getItems().addAll(bookmarksAddSelected, bookmarksRemoveSelected);
        menuTag.getItems().addAll(tagAddSelected, tagRemoveSelected);
        menuExport.getItems().addAll(exportAttachments);
        menuShowIn.getItems().addAll(showInTree);

        getItems().addAll(menuBookmark, menuTag, menuExport, menuShowIn);

        // Listeners
        bookmarksAddSelected.setOnAction((event) -> {
            ObservableList<MessageRow> selectedItems = messagePane.getTable().getSelectionModel().getSelectedItems();

            new Thread(new Task<Object>() {
                @Override
                protected Object call() {
                    messagePane.setLoading(true);
                    selectedItems.forEach(row -> row.getBookmarked().setValue(true));
                    return null;
                }

                @Override
                protected void succeeded() {
                    messagePane.setLoading(false);
                }
            }).start();
        });
        bookmarksRemoveSelected.setOnAction((event) -> {
            ObservableList<MessageRow> selectedItems = messagePane.getTable().getSelectionModel().getSelectedItems();

            new Thread(new Task() {
                @Override
                protected Object call() {
                    messagePane.setLoading(true);
                    selectedItems.forEach(row -> row.getBookmarked().setValue(false));
                    return null;
                }

                @Override
                protected void succeeded() {
                    messagePane.setLoading(false);
                }
            }).start();
        });
        tagAddSelected.setOnAction((event) -> {
            TagStage tagStage = new TagStage();

            tagStage.show();

            tagStage.setOnAddTag((event2) -> {
                messagePane.getTable().getSelectionModel().getSelectedItems().forEach(row -> {
                    row.getTag().setValue(tagStage.getTagName());
                });
                tagStage.close();
            });
        });
        tagRemoveSelected.setOnAction((event) -> {
            messagePane.getTable().getSelectionModel().getSelectedItems().forEach(row -> {
                row.getTag().setValue("");
            });
        });
        exportAttachments.setOnAction((event) -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();

            File selectedDirectory = directoryChooser.showDialog(messagePane.getTable().getScene().getWindow());

            if (selectedDirectory != null) {
                boolean hasAttachments = false;

                for (MessageRow row : messagePane.getTable().getSelectionModel().getSelectedItems()) {
                    MessageValue messageValue = messageFactory.getMessageValue(row.getId());

                    for (AttachmentRow attachment : messageValue.getAttachments()) {
                        try {
                            hasAttachments = true;

                            Files.copy(
                                    attachment.getInputStream(),
                                    Paths.get(selectedDirectory.getPath() + File.separator + attachment.getAttachmentName()),
                                    StandardCopyOption.REPLACE_EXISTING
                            );

                            attachment.getInputStream().reset(); // Important!
                        } catch (IOException ex) {
                            log.error(ex.getMessage(), ex);
                        }
                    }
                }

                if (hasAttachments) {
                    messagePane.displayMessage("Attachments exported successfully.");
                } else {
                    messagePane.displayError("Failed to find attachments to export.");
                }
            }
        });
        showInTree.setOnAction((event) -> {
            MessageRow selectedRow = messagePane.getTable().getSelectionModel().getSelectedItem();

            onShowInTree.show(selectedRow.getFolderID(), selectedRow.getId());
        });
    }

    public interface ShowInTreeEvent {

        void show(String folderID, String messageID);
    }
}
