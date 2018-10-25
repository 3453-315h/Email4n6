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
package com.github.email4n6.view.tabs.tree;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.email4n6.view.messagepane.MessagePaneController;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;
import lombok.extern.slf4j.Slf4j;

/**
 * Controls the tree tab.
 *
 * @author Marten4n6
 */
@Slf4j
public class TreeController {

    private TreeTab treeTab;
    private TreeModel treeModel;
    private ExecutorService executor;

    public TreeController(TreeTab treeTab, TreeModel treeModel) {
        this.treeTab = treeTab;
        this.treeModel = treeModel;
        this.executor = Executors.newSingleThreadExecutor();

        new MessagePaneController(treeTab.getMessagePane(), treeModel.getMessageFactory());

        // Add the created tree items.
        treeModel.getCreatedTreeItems().forEach(treeItem -> {
            treeTab.getRootTreeItem().getChildren().add(treeItem);
        });

        // Catch events fired by the tree tab.
        treeTab.setOnSelectionChange(new SelectionChangeListener());
        treeTab.setOnCheckedChange(new CheckedChangeListener());
    }

    /**
     * Handles tree selection change events.
     */
    class SelectionChangeListener implements ChangeListener<TreeItem<TreeObject>> {
        TreeItem<TreeObject> previousSelection = null;

        @Override
        public void changed(ObservableValue<? extends TreeItem<TreeObject>> observable, TreeItem<TreeObject> oldValue, TreeItem<TreeObject> newValue) {
            boolean isAlreadyChecked = treeTab.getTree().getCheckModel().getCheckedItems().contains(treeTab.getTree().getSelectionModel().getSelectedItem());
            TreeItem<TreeObject> selectedItem = treeTab.getTree().getSelectionModel().getSelectedItem();

            if (!isAlreadyChecked || previousSelection != null) {
                log.debug("Tree selection changed, updating...");

                if (previousSelection != null && !treeTab.getTree().getCheckModel().getCheckedItems().contains(previousSelection)) {
                    // Remove the previous selection from the message pane.
                    log.debug("Removing the previous selection.");

                    executor.submit(treeModel.createTreeTask(
                            treeTab.getMessagePane(), previousSelection, true
                    ));
                }

                if (!treeTab.getTree().getCheckModel().getCheckedItems().contains(selectedItem)) {
                    log.debug("Starting an add task for this folder...");

                    executor.submit(treeModel.createTreeTask(
                            treeTab.getMessagePane(), treeTab.getTree().getSelectionModel().getSelectedItem(),
                            false
                    ));
                }
            }

            previousSelection = treeTab.getTree().getSelectionModel().getSelectedItem();
        }
    }

    /**
     * Handles tree checkbox change events.
     */
    class CheckedChangeListener implements ListChangeListener<TreeItem<TreeObject>> {

        @Override
        public void onChanged(Change<? extends TreeItem<TreeObject>> change) {
            log.debug("Tree checkbox(es) changed, updating...");

            while (change.next()) {
                // Added items
                change.getAddedSubList().forEach(item -> {
                    TreeItem<TreeObject> selectedItem = treeTab.getTree().getSelectionModel().getSelectedItem();

                    if (selectedItem == null || !selectedItem.equals(item)) {
                        log.debug("Starting an \"add\" task for this folder...");

                        executor.submit(treeModel.createTreeTask(
                                treeTab.getMessagePane(), item, false
                        ));
                    }
                });

                // Removed items
                change.getRemoved().forEach(item -> {
                    TreeItem<TreeObject> selectedItem = treeTab.getTree().getSelectionModel().getSelectedItem();
                    boolean wasRunning = false;

                    // Stop any running "add" tasks for this folder...
                    for (Task runningTask : TreeModel.getActiveTasks()) {
                        if (runningTask.toString().equals(item.getValue().getFolderID())) {
                            log.debug("Stopping running \"add\" thread with ID \"{}\".", item.getValue().getFolderID());

                            runningTask.cancel(true);
                            wasRunning = true;

                            treeTab.getMessagePane().setLoading(false);
                        }
                    }

                    // Start a "remove" task for this folder.
                    if (!wasRunning) { // Since if it was running it wouldn't have gotten the chance to add any messages.
                        if (selectedItem == null || !selectedItem.equals(item)) {
                            log.debug("Starting a \"remove\" task for this folder...");

                            executor.submit(treeModel.createTreeTask(
                                    treeTab.getMessagePane(), item, true
                            ));
                        }
                    }
                });
            }
        }
    }
}
