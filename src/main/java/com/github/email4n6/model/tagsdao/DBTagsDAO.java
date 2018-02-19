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

package com.github.email4n6.model.tagsdao;

import com.github.email4n6.model.H2Database;
import com.github.email4n6.model.casedao.Case;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.h2.jdbc.JdbcSQLException;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

/**
 * TagsDAO implementation which stores tags in a database.
 *
 * @author Marten4n6
 */
@Slf4j
public class DBTagsDAO implements TagsDAO {

    private H2Database database;

    public DBTagsDAO(H2Database database) {
        this.database = database;
    }

    @Override
    public String getTag(String id) {
        try {
            @Cleanup Connection connection = database.getDataSource().getConnection();
            @Cleanup PreparedStatement statement = connection.prepareStatement("SELECT * FROM Tags WHERE ID=?");

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

    @Override
    public Set<String> getTagIDs(String tagName) {
        Set<String> ids = new HashSet<>();

        try {
            @Cleanup Connection connection = database.getDataSource().getConnection();
            @Cleanup PreparedStatement statement = connection.prepareStatement("SELECT * FROM Tags WHERE Tag=?");

            statement.setString(1, tagName);

            @Cleanup ResultSet resultSet = statement.executeQuery();

            connection.commit();

            while (resultSet.next()) {
                ids.add(resultSet.getString("ID"));
            }
        } catch (JdbcSQLException ex) {
            // No data available.
            return null;
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        }
        return ids;
    }

    @Override
    public Set<String> getTagNames() {
        // TODO - Looping through every tagged ID is a bad idea.
        Set<String> ids = new HashSet<>();

        try {
            @Cleanup Connection connection = database.getDataSource().getConnection();
            @Cleanup PreparedStatement statement = connection.prepareStatement("SELECT * FROM Tags");
            @Cleanup ResultSet resultSet = statement.executeQuery();

            connection.commit();

            while (resultSet.next()) {
                String tagName = resultSet.getString("Tag");

                ids.add(tagName);
            }
        } catch (JdbcSQLException ex) {
            // No data available.
            return null;
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        }

        log.debug("Returning: {}", ids.toString());
        return ids;
    }

    @Override
    public void setTag(String id, String tagName) {
        try {
            @Cleanup Connection connection = database.getDataSource().getConnection();

            if (tagName.trim().isEmpty()) {
                removeTag(id);
            } else {
                @Cleanup PreparedStatement statement = connection.prepareStatement("MERGE INTO Tags KEY(ID) VALUES (?,?)");

                statement.setString(1, id);
                statement.setString(2, tagName);

                statement.execute();
                connection.commit();
            }
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override
    public void removeTag(String id) {
        try {
            @Cleanup Connection connection = database.getDataSource().getConnection();
            @Cleanup PreparedStatement statement = connection.prepareStatement("DELETE FROM Tags WHERE ID=?");

            statement.setString(1, id);

            statement.execute();
            connection.commit();
        } catch (SQLException ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
