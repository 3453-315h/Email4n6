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

import com.zaxxer.hikari.HikariDataSource;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbc.JdbcSQLException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Model for tags.
 *
 * @author Marten4n6
 */
@Slf4j
public class TagModel {

    private HikariDataSource database;

    public TagModel(HikariDataSource database) {
        this.database = database;
    }

    /**
     * @return The tag of the specified ID otherwise null.
     */
    public String getTag(String id) {
        try {
            @Cleanup Connection connection = database.getConnection();
            @Cleanup PreparedStatement statement = connection.prepareStatement("SELECT * FROM Tags WHERE id=?");

            statement.setString(1, id);

            @Cleanup ResultSet resultSet = statement.executeQuery();

            connection.commit();
            resultSet.next();

            return resultSet.getString("Tag");
        } catch (JdbcSQLException ex) {
            // No data available.
            return null;
        } catch (SQLException ex) {
            log.error(ex.getMessage());
            return null;
        }
    }

    /**
     * Sets the tag of the ID.
     *
     * @param tagName The tag to set.
     */
    public void setTag(String id, String tagName) {
        try {
            @Cleanup Connection connection = database.getConnection();

            if (tagName.trim().isEmpty()) {
                removeTag(id);
            } else {
                @Cleanup PreparedStatement statement = connection.prepareStatement("MERGE INTO Tags KEY(id) VALUES (?,?)");

                statement.setString(1, id);
                statement.setString(2, tagName);

                statement.execute();
                connection.commit();
            }
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * Removes the tag from the ID.
     */
    public void removeTag(String id) {
        try {
            @Cleanup Connection connection = database.getConnection();
            @Cleanup PreparedStatement statement = connection.prepareStatement("DELETE FROM Tags WHERE id=?");

            statement.setString(1, id);

            statement.execute();
            connection.commit();
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
