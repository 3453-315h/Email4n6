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

import com.github.email4n6.message.MessageRow;
import com.github.email4n6.model.tagsdao.TagsDAO;
import com.github.email4n6.view.tabs.bookmarks.BookmarksModel;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

/**
 * The default context menu used by the message pane.
 *
 * @author Marten4n6
 */
@Slf4j
public class DefaultContextMenu extends ContextMenu {

    private MessagePane messagePane;

    public DefaultContextMenu(MessagePane messagePane) {
        this.messagePane = messagePane;

        initComponents();
    }

    private void initComponents() {
        Menu menuBookmark = new Menu("Bookmark");
        Menu menuTag = new Menu("Tag");
        Menu menuExport = new Menu("Export");

        // Bookmark
        MenuItem bookmarksAddSelected = new MenuItem("Add Selected");
        MenuItem bookmarksRemoveSelected = new MenuItem("Remove Selected");

        // Tag
        MenuItem tagAddSelected = new MenuItem("Add Selected");
        MenuItem tagRemoveSelected = new MenuItem("Remove Selected");

        // Add
        menuBookmark.getItems().addAll(bookmarksAddSelected, bookmarksRemoveSelected);
        menuTag.getItems().addAll(tagAddSelected, tagRemoveSelected);

        getItems().addAll(menuBookmark, menuTag, menuExport);

        // Listeners
        bookmarksAddSelected.setOnAction((event) -> {
            ObservableList<MessageRow> selectedItems = messagePane.getTable().getSelectionModel().getSelectedItems();

            new Thread(new Task() {
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
            tagStage.createAndShow();

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
    }
}
