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
import com.zaxxer.hikari.HikariDataSource;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Bookmarks model.
 *
 * @author Marten4n6
 */
@Slf4j
public class BookmarksModel {

    private HikariDataSource database;
    private List<BookmarkListener> listeners;

    public BookmarksModel(HikariDataSource database) {
        this.database = database;
        this.listeners = new ArrayList<>();
    }

    /**
     * @return A list of all bookmarks IDs.
     */
    public List<String> getBookmarks() {
        try {
            List<String> bookmarks = new ArrayList<>();

            @Cleanup Connection connection = database.getConnection();
            @Cleanup Statement statement = connection.createStatement();
            @Cleanup ResultSet resultSet = statement.executeQuery("SELECT * FROM Bookmarks");

            while (resultSet.next()) {
                bookmarks.add(resultSet.getString("ID"));
            }
            connection.commit();

            return bookmarks;
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
            return new ArrayList<>(0);
        }
    }

    /**
     * @return True if this case has bookmarks.
     */
    public boolean hasBookmarks() {
        try {
            @Cleanup Connection connection = database.getConnection();
            @Cleanup Statement statement = connection.createStatement();
            @Cleanup ResultSet resultSet = statement.executeQuery("SELECT TOP(1) 1 FROM Bookmarks");

            connection.commit();
            return resultSet.next();
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * @return True if the ID is a bookmark.
     */
    public boolean isBookmark(String id) {
        try {
            @Cleanup Connection connection = database.getConnection();
            @Cleanup PreparedStatement statement = connection.prepareStatement("SELECT * FROM Bookmarks WHERE id=?");

            statement.setString(1, id);
            @Cleanup ResultSet resultSet = statement.executeQuery();

            connection.commit();
            return resultSet.next();
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * Adds a bookmark.
     */
    public void addBookmark(MessageRow row) {
        try {
            @Cleanup Connection connection = database.getConnection();
            @Cleanup PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO Bookmarks(id) VALUES (?)");

            preparedStatement.setString(1, row.getId());
            preparedStatement.execute();
            connection.commit();

            listeners.forEach(listener -> listener.bookmarkAdded(row));
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * Removes a bookmark.
     */
    public void removeBookmark(MessageRow row) {
        try {
            @Cleanup Connection connection = database.getConnection();
            @Cleanup PreparedStatement statement = connection.prepareStatement("DELETE FROM Bookmarks WHERE id=?");

            statement.setString(1, row.getId());
            statement.execute();
            connection.commit();

            listeners.forEach(listener -> listener.bookmarkRemoved(row));
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * Listener which gets called on bookmark events.
     */
    interface BookmarkListener {

        void bookmarkAdded(MessageRow row);

        void bookmarkRemoved(MessageRow row);
    }

    /**
     * Adds a listener which will be notified on bookmark events.
     *
     * @see BookmarkListener
     */
    void addListener(BookmarkListener bookmarkListener) {
        listeners.add(bookmarkListener);
    }
}
