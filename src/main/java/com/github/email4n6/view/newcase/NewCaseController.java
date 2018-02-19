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

package com.github.email4n6.view.newcase;

import com.github.email4n6.model.casedao.Case;
import com.github.email4n6.parser.spi.ParserFactory;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.*;

/**
 * Handles the interaction between the view and model.
 *
 * @author Marten4n6
 */
@Slf4j
public class NewCaseController {

    private NewCaseStage newCaseStage;
    private NewCaseModel newCaseModel;

    public NewCaseController(NewCaseStage newCaseStage, NewCaseModel newCaseModel) {
        this.newCaseStage = newCaseStage;
        this.newCaseModel = newCaseModel;

        this.newCaseStage.setOnBrowseSource((event -> new BrowseSourceListener().handle(event)));
        this.newCaseStage.setOnCreateCase((event -> new CreateCaseListener().handle(event)));
    }

    /**
     * Handles the browse source event.
     */
    class BrowseSourceListener implements EventHandler {

        @Override
        public void handle(Event event) {
            if (newCaseStage.getSourceType().equals("File(s)")) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.getExtensionFilters().add(getFileExtensionFilter());

                List<File> selectedFiles = fileChooser.showOpenMultipleDialog(newCaseStage.getOwnerWindow());

                if (selectedFiles != null) {
                    StringBuilder sourceText = new StringBuilder();
                    int i = 1;

                    for (File file : selectedFiles) {
                        sourceText.append(file.getName());

                        if (i != selectedFiles.size()) {
                            sourceText.append(", ");
                        }
                        i++;
                    }

                    newCaseStage.getSources().clear();
                    selectedFiles.forEach(file -> newCaseStage.getSources().add(file.getPath()));

                    newCaseStage.setSourceText(sourceText.toString());
                }
            } else {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                File selectedDirectory = directoryChooser.showDialog(newCaseStage.getOwnerWindow());

                if (selectedDirectory != null) {
                    newCaseStage.getSources().clear();
                    newCaseStage.getSources().add(selectedDirectory.getPath());

                    newCaseStage.setSourceText(selectedDirectory.getName());
                }
            }
        }

        /**
         * @return The extension filter that parsers support.
         */
        private FileChooser.ExtensionFilter getFileExtensionFilter() {
            List<String> fileExtensions = new ArrayList<>();

            new ParserFactory().getAllSupportedFileExtensions().forEach(extension -> {
                fileExtensions.add("*." + extension);
            });
            return new FileChooser.ExtensionFilter("Supported", fileExtensions);
        }
    }

    /**
     * Handles the create case event.
     */
    class CreateCaseListener implements EventHandler {

        @Override
        public void handle(Event event) {
            if (newCaseModel.getCaseNames().contains(newCaseStage.getName())) {
                newCaseStage.displayError("A case with that name already exists.");
            } else if (newCaseStage.getName().trim().isEmpty()) {
                newCaseStage.displayError("Invalid case name.");
            } else if (newCaseStage.getInvestigator().trim().isEmpty()) {
                newCaseStage.displayError("Invalid investigator.");
            } else {
                Case createdCase = Case.builder()
                        .name(newCaseStage.getName())
                        .description(newCaseStage.getDescription())
                        .investigator(newCaseStage.getInvestigator())
                        .size("Not calculated yet.") // Calculated when the case first opens.
                        .sources(newCaseStage.getSources())
                        .subFolders(newCaseStage.parseSubFolders())
                        .build();

                newCaseModel.createCase(createdCase);
                newCaseStage.setCreatedCase(createdCase);
                newCaseStage.close();

                log.info("Case \"{}\" created.", newCaseStage.getName());
            }
        }
    }
}
