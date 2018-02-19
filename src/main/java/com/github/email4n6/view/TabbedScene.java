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

package com.github.email4n6.view;

import com.github.email4n6.model.H2Database;
import com.github.email4n6.model.casedao.Case;
import com.github.email4n6.message.factory.DefaultMessageFactory;
import com.github.email4n6.message.factory.MessageFactory;
import com.github.email4n6.model.Indexer;
import com.github.email4n6.model.tagsdao.DBTagsDAO;
import com.github.email4n6.model.tagsdao.TagsDAO;
import com.github.email4n6.parser.view.LoadingStage;
import com.github.email4n6.view.home.HomeController;
import com.github.email4n6.view.home.HomeModel;
import com.github.email4n6.view.home.HomeTab;
import com.github.email4n6.view.tabs.spi.TabFactory;
import com.github.email4n6.view.tabs.bookmarks.BookmarksModel;
import com.github.email4n6.view.tabs.tree.TreeObject;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * This is the main scene which at first only shows the home tab.
 *
 * @author Marten4n6
 */
@Slf4j
public class TabbedScene {

    private @Getter Scene scene;

    public TabbedScene() {
        BorderPane sceneLayout = new BorderPane();
        scene = new Scene(sceneLayout, 900, 600);
        TabPane tabPane = new TabPane();

        // Add the home tabs, other tabs will be added once a case is opened.
        HomeTab homeTab = new HomeTab();
        HomeController homeController = new HomeController(homeTab, new HomeModel());

        homeController.setOnParsingFinished((Case currentCase, Indexer indexer, LoadingStage loadingStage) -> {
            // Parsing finished, add all other tabs.
            loadingStage.setStatus("Closing the indexer...");
            log.debug("Closing the indexer...");

            indexer.commitAndClose();
            log.debug("Indexer closed.");

            log.debug("Shutting down the loading stage...");
            loadingStage.shutdown();
            log.info("Parsing finished.");

            H2Database database = new H2Database(currentCase.getName());
            BookmarksModel bookmarksModel = new BookmarksModel(database);
            TagsDAO tagsDAO = new DBTagsDAO(database);

            MessageFactory messageFactory = new DefaultMessageFactory(currentCase, bookmarksModel, tagsDAO);

            new TabFactory(bookmarksModel, tagsDAO, loadingStage.getCreatedTreeItems()).getTabs().forEach(globalTab -> {
                Platform.runLater(() -> {
                    // Each tab should be able to access the MessageFactory
                    // since it is required to create a MessagePaneController.
                    tabPane.getTabs().add(globalTab.getTab(currentCase, messageFactory));
                });
            });

            Platform.runLater(() -> tabPane.getSelectionModel().selectNext());
        });
        homeController.setOnActiveCaseRemoved((event) -> {
            Platform.runLater(() -> {
                tabPane.getTabs().removeIf(tab -> !tab.getText().equals("Home"));
            });
        });

        tabPane.getTabs().add(homeTab.getTab());
        sceneLayout.setCenter(tabPane);
    }
}
