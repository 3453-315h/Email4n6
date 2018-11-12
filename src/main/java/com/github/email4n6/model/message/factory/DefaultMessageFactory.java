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
package com.github.email4n6.model.message.factory;

import java.util.ArrayList;
import java.util.List;

import com.github.email4n6.model.Case;
import com.github.email4n6.model.message.MessageRow;
import com.github.email4n6.model.message.MessageValue;
import com.github.email4n6.model.parser.pst.PSTMessageFactory;
import com.github.email4n6.view.tabs.bookmarks.BookmarksModel;
import com.github.email4n6.view.tabs.bookmarks.TagModel;
import com.github.email4n6.view.tabs.search.SearchModel;
import com.github.email4n6.view.tabs.tree.TreeObject;

import javafx.scene.control.TreeItem;

/**
 * Default message factory implementation.
 *
 * @author Marten4n6
 */
public class DefaultMessageFactory implements MessageFactory {

    private List<MessageFactory> factories = new ArrayList<>();

    public DefaultMessageFactory(Case currentCase, BookmarksModel bookmarksModel, TagModel tagModel, SearchModel searchModel) {
        factories.add(new PSTMessageFactory(currentCase, bookmarksModel, tagModel, searchModel));
    }

    @Override
    public MessageRow getMessageRow(String id) {
        for (MessageFactory factory : factories) {
            MessageRow messageRow = factory.getMessageRow(id);

            if (messageRow != null) return messageRow;
        }
        return null;
    }

    @Override
    public MessageValue getMessageValue(String id) {
        for (MessageFactory factory : factories) {
            MessageValue messageValue = factory.getMessageValue(id);

            if (messageValue != null) return messageValue;
        }
        return null;
    }

    @Override
    public List<MessageRow> getMessagesFromTreeItem(TreeItem<TreeObject> item) {
        for (MessageFactory factory : factories) {
            List<MessageRow> rows = factory.getMessagesFromTreeItem(item);

            if (rows != null && !rows.isEmpty()) return rows;
        }
        return null;
    }
}
