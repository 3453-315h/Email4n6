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

import com.github.email4n6.model.Case;
import com.github.email4n6.model.Indexer;
import com.github.email4n6.model.message.factory.DefaultMessageFactory;
import com.github.email4n6.view.tabs.bookmarks.BookmarksController;
import com.github.email4n6.view.tabs.bookmarks.BookmarksModel;
import com.github.email4n6.view.tabs.bookmarks.BookmarksTab;
import com.github.email4n6.view.tabs.bookmarks.TagModel;
import com.github.email4n6.view.tabs.home.HomeController;
import com.github.email4n6.view.tabs.home.HomeModel;
import com.github.email4n6.view.tabs.home.HomeTab;
import com.github.email4n6.view.tabs.home.loading.LoadingStage;
import com.github.email4n6.view.tabs.report.ReportController;
import com.github.email4n6.view.tabs.report.ReportModel;
import com.github.email4n6.view.tabs.report.ReportTab;
import com.github.email4n6.view.tabs.search.SearchController;
import com.github.email4n6.view.tabs.search.SearchModel;
import com.github.email4n6.view.tabs.search.SearchTab;
import com.github.email4n6.view.tabs.tree.TreeController;
import com.github.email4n6.view.tabs.tree.TreeModel;
import com.github.email4n6.view.tabs.tree.TreeTab;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * This is the main scene which at first only shows the home tab.
 *
 * @author Marten4n6
 */
@Slf4j
public class TabbedScene {

    private @Getter Scene scene;

    public TabbedScene() {
        TabPane tabPane = new TabPane();

        // Add the home tabs, other tabs will be added once a case is opened.
        HomeTab homeTab = new HomeTab();
        HomeModel homeModel = new HomeModel();

        new HomeController(homeTab, homeModel);

        homeModel.setOnFinishedParsing((Case currentCase, Indexer indexer, LoadingStage loadingStage) -> {
            // Parsing finished, add all other tabs.
            loadingStage.setStatus("Closing the indexer...");
            log.info("Closing the indexer...");

            indexer.close();
            log.info("Indexer closed.");

            log.info("Shutting down the loading stage...");
            loadingStage.shutdown();
            log.info("Parsing finished.");

            // Models
            BookmarksModel bookmarksModel = new BookmarksModel(homeModel.getDatabase());
            TagModel tagModel = new TagModel(homeModel.getDatabase());
            SearchModel searchModel = new SearchModel(homeModel.getCurrentCase().getName());

            DefaultMessageFactory messageFactory = new DefaultMessageFactory(homeModel.getCurrentCase(), bookmarksModel, tagModel, searchModel);

            Platform.runLater(() -> {
                // Initialize tabs...
                TreeTab treeTab = new TreeTab();
                SearchTab searchTab = new SearchTab();
                BookmarksTab bookmarksTab = new BookmarksTab();
                ReportTab reportTab = new ReportTab();

                // Controllers
                new TreeController(treeTab, new TreeModel(messageFactory, loadingStage.getCreatedTreeItems()));
                new SearchController(searchTab, searchModel, messageFactory, homeModel.getCurrentCase());
                new BookmarksController(bookmarksTab, bookmarksModel, messageFactory);
                new ReportController(reportTab, new ReportModel(bookmarksModel, tagModel, messageFactory, homeModel.getCurrentCase()));

                tabPane.getTabs().addAll(
                        treeTab.getTab(), searchTab.getTab(),
                        bookmarksTab.getTab(), reportTab.getTab()
                );
                tabPane.getSelectionModel().selectNext();
            });
        });
        homeModel.setOnActiveCaseClosed((event) -> {
            Platform.runLater(() -> {
                tabPane.getTabs().removeIf(tab -> !tab.getText().equals("Home"));
            });
        });

        tabPane.getTabs().add(homeTab.getTab());
        
        BorderPane root = new BorderPane();
        root.setCenter(tabPane);
        scene = new Scene(root, 900, 600);
    }
}
