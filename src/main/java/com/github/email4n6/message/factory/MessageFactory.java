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

package com.github.email4n6.message.factory;

import com.github.email4n6.message.MessageRow;
import com.github.email4n6.message.MessageValue;
import com.github.email4n6.view.tabs.tree.TreeObject;
import javafx.scene.control.TreeItem;

import java.util.List;

/**
 * Factory for creating messages.
 *
 * @author Marten4n6
 */
public interface MessageFactory {

    /**
     * @param id          The message ID.
     * @return A row that can be added to the message table.
     * @see MessageRow
     */
    MessageRow getMessageRow(String id);

    /**
     * @param id          The message ID.
     * @return A message value row (body, headers, attachments etc.) otherwise null.
     * @see MessageValue
     */
    MessageValue getMessageValue(String id);

    /**
     * @return A list of messages from the specified tree item, may return null.
     */
    List<MessageRow> getMessagesFromTreeItem(TreeItem<TreeObject> treeItem);
}
