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

package com.github.email4n6.view.tabs.search;

import com.github.email4n6.model.Case;
import com.github.email4n6.model.Settings;
import com.github.email4n6.model.Version;
import com.github.email4n6.model.message.MessageRow;
import com.github.email4n6.model.message.factory.MessageFactory;
import com.github.email4n6.view.messagepane.MessagePaneController;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This class handles updating the MessagePane with the search results.
 *
 * @author Marten4n6
 */
@Slf4j
public class SearchController {

    private SearchTab searchTab;
    private SearchModel searchModel;
    private MessageFactory messageFactory;
    private Case currentCase;

    private Task<List<MessageRow>> worker;

    public SearchController(SearchTab searchTab, SearchModel searchModel, MessageFactory messageFactory, Case currentCase) {
        this.searchTab = searchTab;
        this.searchModel = searchModel;
        this.messageFactory = messageFactory;
        this.currentCase = currentCase;

        new MessagePaneController(searchTab.getMessagePane(), messageFactory);

        // Catch events fired by the search tab.
        searchTab.setOnSearch(new SearchListener());
        searchTab.setOnSettingsClicked(new SettingsListener());
    }

    /**
     * Handles search requests.
     */
    class SearchListener implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent event) {
            if (searchTab.getSearchQuery().isEmpty()) return;
            searchTab.getMessagePane().setLoading(true);

            if (worker != null && !worker.isDone()) {
                // Stop previous search query.
                log.debug("Stopping previous search query...");

                worker.cancel(true);
            }

            searchTab.setLoading(true);
            searchTab.getMessagePane().clear();
            searchTab.getMessagePane().getTable().getItems().clear();

            worker = new Task<List<MessageRow>>() {
                @Override
                protected List<MessageRow> call() {
                    log.info("Searching for: {}...", searchTab.getSearchQuery());

                    int searchLimit = Integer.parseInt(Settings.get(currentCase.getName(), "search_limit"));
                    if (searchLimit == 0) {
                        log.warn("Showing search results without a limit!");
                        searchLimit = Integer.MAX_VALUE;
                    }

                    List<MessageRow> messages = new ArrayList<>();
                    long startTime = System.currentTimeMillis();

                    for (Document document : searchModel.search(searchTab.getSearchQuery(), searchLimit)) {
                        if (isCancelled()) {
                            log.debug("Search query stopped.");
                            return new ArrayList<>(0);
                        }

                        messages.add(messageFactory.getMessageRow(document.get("id")));
                    }

                    long endTime = System.currentTimeMillis();
                    String secondsTaken = new DecimalFormat("#0.00000").format((endTime - startTime) / 1000d);

                    log.info("Returning {} result(s) after {}ms ({} seconds).", messages.size(), (endTime - startTime), secondsTaken);
                    return messages;
                }

                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        searchTab.getMessagePane().getTable().getItems().addAll(getValue());
                        searchTab.setLoading(false);
                    });
                }
            };

            new Thread(worker).start();
        }
    }

    /**
     * Handles when the settings label is clicked.
     */
    class SettingsListener implements EventHandler<MouseEvent> {

        @Override
        public void handle(MouseEvent event) {
            String searchLimit = Settings.get(currentCase.getName(), "search_limit");

            TextInputDialog dialog = new TextInputDialog(searchLimit);
            dialog.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
            dialog.setHeaderText("Search limit");

            Optional<String> result = dialog.showAndWait();

            if (result.isPresent()) {
                log.debug("Changing search limit to: {}", result.get());

                Settings.set(currentCase.getName(), "search_limit", result.get());
                searchTab.reload();
            }
        }
    }
}
