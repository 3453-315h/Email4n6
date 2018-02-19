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

import com.github.email4n6.model.casedao.Case;
import com.github.email4n6.message.factory.MessageFactory;
import com.github.email4n6.view.messagepane.MessagePane;
import com.github.email4n6.view.messagepane.MessagePaneController;
import com.github.email4n6.view.tabs.spi.GlobalTab;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import lombok.Setter;

/**
 * The bookmark tab.
 *
 * @author Marten4n6
 */
public class BookmarksTab implements GlobalTab {

    private @Getter MessageFactory messageFactory;
    private @Getter MessagePane messagePane;

    // Lets the controller know we're good to go so
    // that it can add existing bookmarks.
    private @Setter EventHandler<ActionEvent> onInitialize;

    @Override
    public Tab getTab(Case currentCase, MessageFactory messageFactory) {
        this.messageFactory = messageFactory;

        Tab tab = new Tab();
        BorderPane tabLayout = new BorderPane();

        messagePane = new MessagePane(currentCase);
        new MessagePaneController(messagePane, messageFactory);

        // Tab
        tab.setText("Bookmarks");
        tab.setClosable(false);
        tab.setContent(tabLayout);
        tab.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/bookmark.png"))));

        tabLayout.setCenter(messagePane.getPane());

        this.onInitialize.handle(new ActionEvent());
        return tab;
    }
}
