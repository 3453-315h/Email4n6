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
package com.github.email4n6.view.messagepane;

import com.github.email4n6.model.Version;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

/**
 * A simple stage for adding tags.
 *
 * @author Marten4n6
 */
public class TagStage {

    private Stage stage;

    private @Getter String tagName;
    private @Setter EventHandler<ActionEvent> onAddTag;

    /**
     * Initializes the tag stage.
     */
    TagStage() {
        stage = new Stage();
        GridPane gridPane = new GridPane();
        Scene scene = new Scene(gridPane, 370, 100);

        // Stage
        stage.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
        stage.setResizable(false);

        // Grid Pane
        gridPane.setPadding(new Insets(5, 5, 5, 5));
        gridPane.setHgap(5);

        // Text Field
        TextField textField = new TextField();

        GridPane.setHgrow(textField, Priority.ALWAYS);

        // Button
        Button button = new Button("Add tag");

        // Add
        gridPane.add(textField, 1, 0);
        gridPane.add(button, 2, 0);

        stage.setScene(scene);

        // Listeners
        button.setOnAction((event) -> {
            tagName = textField.getText();
            onAddTag.handle(event);
        });
    }

    public void show() {
        stage.centerOnScreen();
        stage.show();
    }

    public void close() {
        stage.close();
    }
}
