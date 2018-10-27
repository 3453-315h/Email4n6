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

import com.github.email4n6.view.messagepane.MessagePane;

import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import lombok.Getter;

/**
 * The bookmark tab.
 *
 * @author Marten4n6
 */
public class BookmarksTab {

    private @Getter Tab tab;
    private @Getter MessagePane messagePane;

    /**
     * Initializes the bookmarks tab.
     */
    public BookmarksTab() {
        tab = new Tab();
        BorderPane tabLayout = new BorderPane();

        // Tab
        tab.setText("Bookmarks");
        tab.setClosable(false);
        tab.setContent(tabLayout);
        tab.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/star.png"))));

        messagePane = new MessagePane();
        tabLayout.setCenter(messagePane.getPane());
    }
}
