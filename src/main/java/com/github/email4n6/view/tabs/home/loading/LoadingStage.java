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
package com.github.email4n6.view.tabs.home.loading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.email4n6.model.Version;
import com.github.email4n6.view.tabs.home.loading.taskprogress.TaskProgressView;
import com.github.email4n6.view.tabs.tree.TreeObject;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;

/**
 * Stage that gets shown when opening a case, used by parsers to show progress.
 * Parsers may also communicate objects to others tabs via this stage (like the tree).
 *
 * @author Marten4n6
 */
public class LoadingStage {

    /**
     * Parsers append their tree to this list, used by the tree tab.
     */
    private @Getter List<TreeItem<TreeObject>> createdTreeItems;

    private Stage stage;
    private Label statusLabel;
    private TaskProgressView progressView;

    private ExecutorService executorService;

    public LoadingStage(Window ownerWindow) {
        stage = new Stage();
        createdTreeItems = new ArrayList<>();

        BorderPane sceneLayout = new BorderPane();
        Scene scene = new Scene(sceneLayout, 485, 255);

        // Label
        statusLabel = new Label("Status: Waiting for parsers to finish...");
        statusLabel.setPadding(new Insets(0, 0, 5, 0));

        // Task progress
        progressView = new TaskProgressView();
        executorService = Executors.newSingleThreadExecutor();

        // Layout
        sceneLayout.setTop(statusLabel);
        sceneLayout.setCenter(progressView);
        sceneLayout.setPadding(new Insets(5, 5, 5, 5));

        stage.setScene(scene);
        stage.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(ownerWindow);
    }

    /**
     * Shows the loading stage.
     */
    public void show() {
        stage.centerOnScreen();
        stage.show();
    }

    /**
     * Closes the loading stage.
     */
    public void shutdown() {
        executorService.shutdown();
        Platform.runLater(() -> stage.close());
    }

    /**
     * Sets the text of the status label.
     */
    public void setStatus(String message) {
        Platform.runLater(() -> statusLabel.setText("Status: " + message));
    }

    /**
     * Starts the task and adds it to the loading view.
     */
    public void addTask(Task task) {
        progressView.getTasks().add(task);
        task.setOnSucceeded((event) -> createdTreeItems.add((TreeItem<TreeObject>) task.getValue()));

        executorService.submit(task);
    }
}
