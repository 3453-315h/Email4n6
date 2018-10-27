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

import com.github.email4n6.model.Version;
import com.github.email4n6.utils.PathUtils;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;

/**
 * Shown when a user opens this application for the first time.
 *
 * @author Marten4n6
 */
public class StartupStage {

    private Stage stage;
    private @Getter boolean confirmed;

    /**
     * Initializes the stage.
     */
    public StartupStage() {
        stage = new Stage();

        BorderPane layout = new BorderPane();
        Scene scene = new Scene(layout, 750, 360);

        Label textLabel = new Label("Email4n6 will live in the current directory: \n" + PathUtils.getApplicationPath() +  "\n\nAre you sure you want to continue?");
        Label logoLabel = new Label();

        logoLabel.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/logo-startup.png"))));
        logoLabel.setContentDisplay(ContentDisplay.RIGHT);

        Button buttonNo = new Button("No");
        Button buttonYes = new Button("Yes");

        buttonNo.setMinWidth(100);
        buttonNo.setFocusTraversable(false);
        buttonYes.setMinWidth(100);
        buttonYes.setDefaultButton(true);

        HBox buttonLayout = new HBox(buttonNo, buttonYes);
        buttonLayout.setSpacing(5);
        buttonLayout.setAlignment(Pos.CENTER);

        VBox centerLayout = new VBox();
        centerLayout.getChildren().addAll(textLabel, buttonLayout);
        centerLayout.setSpacing(15);
        centerLayout.setAlignment(Pos.CENTER);

        // Add
        layout.setCenter(centerLayout);
        layout.setRight(logoLabel);
        layout.setPadding(new Insets(5, 5, 5, 5));

        buttonNo.setOnAction((event) -> {
            confirmed = false;
            stage.close();
        });
        buttonYes.setOnAction((event) -> {
            confirmed = true;
            stage.close();
        });

        stage.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
        stage.initOwner(scene.getWindow());
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
    }

    /**
     * Shows the stage and waits for the user input.
     */
    public void showAndWait() {
        stage.centerOnScreen();
        stage.showAndWait();
    }
}
