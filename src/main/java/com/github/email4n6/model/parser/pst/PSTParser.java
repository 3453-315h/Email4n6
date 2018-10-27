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

package com.github.email4n6.model.parser.pst;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.email4n6.model.Settings;
import com.github.email4n6.model.parser.Parser;
import com.github.email4n6.model.parser.ParserConfiguration;
import com.github.email4n6.view.tabs.tree.TreeObject;
import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTObject;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeItem;
import lombok.extern.slf4j.Slf4j;

/**
 * Parser implementation which parses PST files.
 *
 * @author Marten4n6
 * @see <a href="https://github.com/rjohnsondev/java-libpst">java-libpst</a>
 */
@Slf4j
public class PSTParser implements Parser {

    // TODO -
    // Instead of a global flag, SHA256 hash each file?
    // This way we can support files that were moved or when new files are added.
    private boolean isParsed;
    private TreeItem<TreeObject> createdTree;

    public PSTParser() {
        PSTMessageFactory.clearPSTFiles();
    }

    @Override
    public String getName() {
        return "PSTParser";
    }

    @Override
    public Set<String> getSupportedFileExtensions() {
        Set<String> extensions = new HashSet<>();
        extensions.add("pst");
        extensions.add("ost"); // Untested

        return extensions;
    }

    @Override
    public TreeItem<TreeObject> parseFile(File file, ParserConfiguration configuration, int totalFiles) {
        isParsed = Boolean.parseBoolean(Settings.get(configuration.getCurrentCase().getName(), getName() + "-IsParsed"));

        Task task = new Task<Object>() {
            private AtomicInteger messageAmount = new AtomicInteger(0);
            private AtomicInteger finishedAmount = new AtomicInteger(0);

            private PSTIndexer indexer;
            private CheckBoxTreeItem<TreeObject> rootTreeItem;

            @Override
            protected CheckBoxTreeItem<TreeObject> call() throws Exception {
                PSTFile pstFile = new PSTFile(file);
                PSTFolder rootFolder = pstFile.getRootFolder();

                String pstFileID = IDGenerator.getID(pstFile);

                updateProgress(0, Long.MAX_VALUE);

                // So we can retrieve this PSTFile later on.
                PSTMessageFactory.addPSTFile(pstFile);

                if (!isParsed) {
                    log.info("Parsing {} ({})...", file.getName(), file.getPath());
                    indexer = new PSTIndexer(configuration);

                    updateTitle(getName());
                    updateProgress(0, Long.MAX_VALUE);

                    updateMessage("Getting message amount...");
                    calculateMessageAmount(pstFile.getRootFolder());

                    updateProgress(0, messageAmount.get());
                    updateMessage("Indexing: " + file.getName());
                } else {
                    log.info("This file is already parsed.");
                }

                rootTreeItem = new CheckBoxTreeItem<>(new TreeObject(file.getName(), null));

                for (PSTFolder subFolder : rootFolder.getSubFolders()) {
                    processFolder(subFolder, pstFileID, rootTreeItem);
                }
                return rootTreeItem;
            }

            /**
             * Updates the messageAmount variable to the amount of messages in the PSTFolder.
             */
            private void calculateMessageAmount(PSTFolder folder) {
                try {
                    if (folder.getContentCount() != 0 && folder.getNodeType() != 3) {
                        messageAmount.getAndAdd(folder.getContentCount());
                    }

                    if (folder.getNodeType() != 3) {
                        for (PSTFolder pstFolder : folder.getSubFolders()) {
                            calculateMessageAmount(pstFolder);
                        }
                    }
                } catch (IOException | PSTException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }

            /**
             * Recursively processes all folders and subfolders.
             */
            private void processFolder(PSTFolder folder, String pstFileID, CheckBoxTreeItem<TreeObject> treeItem) {
                try {
                    log.debug("Processing folder \"{}\" with {} messages...", folder.getDisplayName(), folder.getEmailCount());

                    // Tree
                    String treeItemTitle = folder.getEmailCount() != -1 && folder.getEmailCount() != 0 ? folder.getDisplayName() + " (" + folder.getEmailCount() + ")" : folder.getDisplayName();
                    CheckBoxTreeItem<TreeObject> folderTreeItem = new CheckBoxTreeItem<>(new TreeObject(treeItemTitle, IDGenerator.getID(folder)));

                    treeItem.getChildren().add(folderTreeItem);

                    if (!isParsed && folder.getContentCount() > 0) {

                        PSTObject pstObject;
                        String folderID = IDGenerator.getID(folder);

                        while ((pstObject = folder.getNextChild()) != null) {
                            // Do stuff with the PSTObject here...
                            indexer.index(pstObject, pstFileID, folderID);

                            finishedAmount.incrementAndGet();
                            updateProgress(finishedAmount.get(), messageAmount.get());
                        }
                    }

                    // Recursive loop through all subfolders.
                    if (folder.getNodeType() != 3) {
                        for (PSTFolder subFolder : folder.getSubFolders()) {
                            processFolder(subFolder, pstFileID, folderTreeItem);
                        }
                    } else {
                        log.debug("Skipping subfolders of \"{}\"...", folder.getDisplayName());
                    }
                } catch (IOException | PSTException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected void succeeded() {
                log.info("Finished parsing.");

                Settings.set(configuration.getCurrentCase().getName(), getName() + "-IsParsed", "true");
                configuration.getFinishedListener().handle(new ActionEvent());
            }

            @Override
            protected void failed() {
                Throwable exception = getException();

                if (exception.getMessage() != null && exception.getMessage().contains("Invalid file header")) {
                    log.error("Invalid or corrupt PST file: {} ({})", file.getName(), file.getPath(), exception);
                } else {
                    log.error("Failed with unknown exception: ", exception);
                }

                configuration.getFinishedListener().handle(new ActionEvent());
            }
        };
        task.setOnScheduled((event) -> {
            log.debug("Assigning created tree...");

            createdTree = (TreeItem<TreeObject>)task.getValue();
        });

        configuration.getLoadingStage().addTask(task);
        log.debug("Returning: {}", createdTree);

        return createdTree;
    }
}
