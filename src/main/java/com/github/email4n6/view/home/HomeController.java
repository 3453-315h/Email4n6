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

package com.github.email4n6.view.home;

import com.github.email4n6.model.H2Database;
import com.github.email4n6.model.casedao.Case;
import com.github.email4n6.model.casedao.CaseDAO;
import com.github.email4n6.model.casedao.JSONCaseDAO;
import com.github.email4n6.parser.FileParser;
import com.github.email4n6.parser.spi.ParserFactory;
import com.github.email4n6.parser.view.LoadingStage;
import com.github.email4n6.view.newcase.NewCaseController;
import com.github.email4n6.view.newcase.NewCaseModel;
import com.github.email4n6.view.newcase.NewCaseStage;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Handles the interaction between the view and model.
 *
 * @author Marten4n6
 */
@Slf4j
public class HomeController {

    private HomeTab homeTab;
    private HomeModel homeModel;

    private @Getter String openedCase;

    private @Setter FileParser.FinishedListener onParsingFinished;
    private @Setter EventHandler<ActionEvent> onActiveCaseRemoved;

    public HomeController(HomeTab homeTab, HomeModel homeModel) {
        this.homeTab = homeTab;
        this.homeModel = homeModel;

        // Add existing cases to the home tab.
        CaseDAO caseDAO = new JSONCaseDAO();

        caseDAO.findAll().forEach(caseObject -> {
            homeTab.getTable().getItems().add(caseObject);
        });

        this.homeTab.setOnOpenCase((event) -> new OpenCaseListener().handle(event));
        this.homeTab.setOnNewCase((event) -> new NewCaseListener().handle(event));
        this.homeTab.setOnRemoveCase((event) -> new RemoveCaseListener().handle(event));
    }

    /**
     * Handles the open case event.
     */
    class OpenCaseListener implements EventHandler {

        @Override
        public void handle(Event event) {
            Case selectedCase = homeTab.getTable().getSelectionModel().getSelectedItem();

            if (selectedCase == null) {
                homeTab.displayError("No case selected.");
            } else if (openedCase != null && openedCase.equals(selectedCase.getName())) {
                homeTab.displayError("This case is already open.");
            } else {
                Optional<ButtonType> confirmOpen = homeTab.displayConfirmation("Are you sure you want to open this case?");

                if (confirmOpen.isPresent() && confirmOpen.get() == ButtonType.YES) {
                    log.info("Opening case \"{}\"...", selectedCase.getName());

                    openedCase = selectedCase.getName();
                    Window ownerWindow = homeTab.getTable().getScene().getWindow();
                    LoadingStage loadingStage = new LoadingStage(ownerWindow);

                    ParserFactory parserFactory = new ParserFactory();
                    FileParser fileParser = new FileParser(parserFactory, selectedCase, loadingStage);

                    loadingStage.show();

                    new Thread(new Task<String>() {
                        @Override
                        protected String call() {
                            String sourceSize = null;

                            if (selectedCase.getSize().startsWith("Not calculated")) {
                                loadingStage.setStatus("Calculating the size of the source(s)...");
                                log.debug("Calculating the size of the source(s)...");

                                sourceSize = homeModel.getSourceSize(
                                        selectedCase.getSources(),
                                        parserFactory.getAllSupportedFileExtensions(),
                                        selectedCase.isSubFolders()
                                );

                                log.debug("Source(s) size: {}", sourceSize);
                                loadingStage.setStatus("Waiting for parsers to finish...");
                            }

                            fileParser.setOnParsingFinished(onParsingFinished); // Send through to whoever is listening.

                            File firstSource = new File(selectedCase.getSources().iterator().next());

                            if (firstSource.isDirectory()) {
                                fileParser.parseFolder(firstSource.getPath(), selectedCase.isSubFolders());
                            } else {
                                fileParser.parseFiles(selectedCase.getSources());
                            }
                            return sourceSize;
                        }

                        @Override
                        protected void succeeded() {
                            try {
                                String sourceSize = get();

                                if (sourceSize != null) {
                                    homeModel.updateCaseSize(selectedCase, sourceSize);
                                    homeTab.getTable().refresh();
                                }
                            } catch (InterruptedException | ExecutionException ex) {
                                log.error(ex.getMessage(), ex);
                            }
                        }
                    }).start();
                }
            }
        }
    }

    /**
     * Handles the new case event.
     */
    class NewCaseListener implements EventHandler {

        @Override
        public void handle(Event event) {
            Window ownerWindow = homeTab.getTable().getScene().getWindow();

            NewCaseStage newCaseStage = new NewCaseStage(ownerWindow);
            new NewCaseController(newCaseStage, new NewCaseModel());

            newCaseStage.showAndWait(); // Waits to be closed by the NewCaseController.

            Platform.runLater(() -> {
                if (newCaseStage.getCreatedCase() != null) {
                    homeTab.getTable().getItems().add(newCaseStage.getCreatedCase());
                }
            });
        }
    }

    /**
     * Handles the remove case event.
     */
    class RemoveCaseListener implements EventHandler {

        @Override
        public void handle(Event event) {
            Case selectedCase = homeTab.getTable().getSelectionModel().getSelectedItem();

            if (selectedCase == null) {
                homeTab.displayError("No case selected.");
            } else {
                Optional<ButtonType> confirmRemove = homeTab.displayConfirmation("Are you sure you want to remove this case?");

                if (confirmRemove.isPresent() && confirmRemove.get() == ButtonType.YES) {
                    if (openedCase != null && openedCase.equals(selectedCase.getName())) {
                        // Let the TabbedScene handle the removal of the tabs first.
                        onActiveCaseRemoved.handle(new ActionEvent());
                        openedCase = null;
                    }

                    CaseDAO caseDAO = new JSONCaseDAO();

                    caseDAO.deleteCase(selectedCase.getName());
                    Platform.runLater(() -> homeTab.getTable().getItems().remove(selectedCase));
                }
            }
        }
    }
}
