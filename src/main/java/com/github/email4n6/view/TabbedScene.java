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
import com.github.email4n6.model.message.MessageRow;
import com.github.email4n6.model.message.factory.DefaultMessageFactory;
import com.github.email4n6.model.message.factory.MessageFactory;
import com.github.email4n6.view.messagepane.DefaultContextMenu;
import com.github.email4n6.view.messagepane.MessagePaneController;
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
import com.github.email4n6.view.tabs.tree.TreeObject;
import com.github.email4n6.view.tabs.tree.TreeTab;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;

/**
 * This is the main scene which at first only shows the home tab.
 *
 * @author Marten4n6
 */
@Slf4j
public class TabbedScene {

    private @Getter
    Scene scene;

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

            Platform.runLater(() -> {
                // Model
                BookmarksModel bookmarksModel = new BookmarksModel(homeModel.getDatabase());
                TagModel tagModel = new TagModel(homeModel.getDatabase());
                SearchModel searchModel = new SearchModel(currentCase.getName());

                MessageFactory messageFactory = new DefaultMessageFactory(currentCase, bookmarksModel, tagModel, searchModel);

                TreeModel treeModel = new TreeModel(messageFactory, loadingStage.getCreatedTreeItems());
                ReportModel reportModel = new ReportModel(bookmarksModel, tagModel, messageFactory, currentCase);

                // View
                TreeTab treeTab = new TreeTab();
                SearchTab searchTab = new SearchTab();
                BookmarksTab bookmarksTab = new BookmarksTab();
                ReportTab reportTab = new ReportTab();

                // Controller
                new TreeController(treeTab, treeModel);
                new SearchController(searchTab, searchModel, messageFactory);
                new BookmarksController(bookmarksTab, bookmarksModel, messageFactory);
                new ReportController(reportTab, reportModel);

                MessagePaneController treeMessagePaneController = new MessagePaneController(treeTab.getMessagePane(), messageFactory);
                MessagePaneController searchMessagePaneController = new MessagePaneController(searchTab.getMessagePane(), messageFactory);
                MessagePaneController bookmarksMessagePaneController = new MessagePaneController(bookmarksTab.getMessagePane(), messageFactory);

                // Listeners
                DefaultContextMenu.ShowInTreeEvent onShowInTree = (String folderID, String messageID) -> {
                    TreeItem<TreeObject> folder = getTreeViewItem(treeTab.getRootTreeItem(), folderID);
                    TreeItem<TreeObject> selectedFolder = treeTab.getTree().getSelectionModel().getSelectedItem();

                    Platform.runLater(() -> {
                        // Select folder
                        CountDownLatch countDownLatch = new CountDownLatch(1);

                        treeModel.setMessagesAddedLatch(countDownLatch);
                        tabPane.getSelectionModel().select(treeTab.getTab());
                        treeTab.getTree().getSelectionModel().select(folder);

                        new Thread(() -> {
                            try {
                                if (selectedFolder == null || !selectedFolder.equals(folder)) {
                                    // Wait for messages to be added.
                                    log.debug("Waiting for messages to be added...");
                                    countDownLatch.await();
                                }
                                
                                // Select message
                                Platform.runLater(() -> {
                                    for (MessageRow messageRow : treeTab.getMessagePane().getTable().getItems()) {
                                        if (messageRow.getId().equals(messageID)) {
                                            treeTab.getMessagePane().getTable().getSelectionModel().select(messageRow);
                                            treeTab.getMessagePane().getTable().scrollTo(messageRow);
                                            break;
                                        }
                                    }
                                });
                            } catch (InterruptedException ex) {
                                log.error(ex.getMessage(), ex);
                            }
                        }).start();
                    });
                };

                treeMessagePaneController.getDefaultContextMenu().setOnShowInTree(onShowInTree);
                searchMessagePaneController.getDefaultContextMenu().setOnShowInTree(onShowInTree);
                bookmarksMessagePaneController.getDefaultContextMenu().setOnShowInTree(onShowInTree);

                tabPane.getTabs().add(treeTab.getTab());
                tabPane.getTabs().add(searchTab.getTab());
                tabPane.getTabs().add(bookmarksTab.getTab());
                tabPane.getTabs().add(reportTab.getTab());
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

    private TreeItem<TreeObject> getTreeViewItem(TreeItem<TreeObject> item, String value) {
        if (item.getValue().getFolderID() != null && item.getValue().getFolderID().equals(value)) {
            return item;
        }

        for (TreeItem<TreeObject> child : item.getChildren()) {
            TreeItem<TreeObject> s = getTreeViewItem(child, value);

            if (s != null) {
                return s;
            }
        }
        return null;
    }

}
