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
package com.github.email4n6.view.tabs.home;

import java.util.Optional;

import com.github.email4n6.model.Case;
import com.github.email4n6.model.parser.FileParser;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

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
        homeModel.getCases().forEach(caseName -> {
            homeTab.getTable().getItems().add(homeModel.getCaseObject(caseName));
        });

        homeTab.setOnCreateCase(new CreateCaseListener());
        homeTab.setOnOpenCase(new OpenCaseListener());
        homeTab.setOnRemoveCase(new RemoveCaseListener());
    }

    /**
     * Handles the new case event.
     */
    class CreateCaseListener implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent event) {
            Window ownerWindow = homeTab.getTable().getScene().getWindow();
            HomeTab.CreateCaseStage createCaseStage = homeTab.new CreateCaseStage(ownerWindow);

            createCaseStage.show();

            createCaseStage.setOnCreateCase((event2) -> {
                if (homeModel.isExistingCase(createCaseStage.getCreatedCase().getName())) {
                    createCaseStage.displayError("A case with this name already exists.");
                } else {
                    homeTab.getTable().getItems().add(createCaseStage.getCreatedCase());
                    createCaseStage.close();

                    homeModel.openCase(createCaseStage.getCreatedCase(), homeTab.getTable().getScene().getWindow());
                }
            });
        }
    }

    /**
     * Handles the open case event.
     */
    class OpenCaseListener implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent event) {
            Case selectedCase = homeTab.getTable().getSelectionModel().getSelectedItem();

            if (selectedCase == null) {
                homeTab.displayError("No case selected.");
            } else if (openedCase != null && openedCase.equals(selectedCase.getName())) {
                homeTab.displayError("This case is already open.");
            } else {
                Optional<ButtonType> confirmOpen = homeTab.displayConfirmation("Are you sure you want to open this case?");

                if (confirmOpen.isPresent() && confirmOpen.get() == ButtonType.YES) {
                    log.info("Opening case \"{}\"...", selectedCase.getName());

                    homeModel.openCase(selectedCase, homeTab.getTable().getScene().getWindow());
                }
            }
        }
    }

    /**
     * Handles the remove case event.
     */
    class RemoveCaseListener implements EventHandler<ActionEvent> {

        @Override
        public void handle(ActionEvent event) {
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

                    homeModel.removeCase(selectedCase.getName());
                    Platform.runLater(() -> homeTab.getTable().getItems().remove(selectedCase));
                }
            }
        }
    }
}
