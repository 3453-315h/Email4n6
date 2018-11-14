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
package com.github.email4n6.view.tabs.bookmarks;

import com.github.email4n6.model.message.MessageRow;
import com.github.email4n6.model.message.factory.MessageFactory;
import com.github.email4n6.view.messagepane.MessagePaneController;

import javafx.application.Platform;

/**
 * Controls the bookmarks tab.
 *
 * @author Marten4n6
 */
public class BookmarksController {

    private BookmarksTab bookmarksTab;
    private BookmarksModel bookmarksModel;

    public BookmarksController(BookmarksTab bookmarksTab, BookmarksModel bookmarksModel, MessageFactory messageFactory) {
        this.bookmarksTab = bookmarksTab;
        this.bookmarksModel = bookmarksModel;

        // Add existing bookmarks.
        bookmarksModel.getBookmarks().forEach(bookmarkID -> {
            MessageRow row = messageFactory.getMessageRow(bookmarkID);

            bookmarksTab.getMessagePane().getTable().getItems().add(row);
        });

        bookmarksModel.addListener(new BookmarkListener());
    }

    /**
     * Handles the bookmark listener events.
     */
    class BookmarkListener implements BookmarksModel.BookmarkListener {

        @Override
        public void bookmarkAdded(MessageRow row) {
            Platform.runLater(() -> {
                bookmarksTab.getMessagePane().getTable().getItems().add(row);
            });
        }

        @Override
        public void bookmarkRemoved(MessageRow row) {
            Platform.runLater(() -> {
                if (bookmarksTab.getMessagePane().getTable().getSelectionModel().getSelectedItem() != null
                    && bookmarksTab.getMessagePane().getTable().getSelectionModel().getSelectedItem().equals(row)) {
                    bookmarksTab.getMessagePane().clear();
                }

                bookmarksTab.getMessagePane().getTable().getItems().remove(row);
            });
        }
    }
}
