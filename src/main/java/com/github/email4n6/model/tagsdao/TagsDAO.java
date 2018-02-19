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

import java.util.Set;

/**
 * Tags DAO interface.
 *
 * @author Marten4n6
 */
public interface TagsDAO {

    /**
     * @return The tag of the specified ID otherwise null.
     */
    String getTag(String id);

    /**
     * @return A set of all message IDs that are tagged.
     *
     * @param tagName The tag to retrieve all IDs from.
     */
    Set<String> getTagIDs(String tagName);

    /**
     * @return A set of all tag names.
     */
    Set<String> getTagNames();

    /**
     * Sets the tag of the ID.
     *
     * @param tagName The tag to set, may be an empty string to remove the tag.
     */
    void setTag(String id, String tagName);

    /**
     * Removes the tag from the ID.
     */
    void removeTag(String id);
}
