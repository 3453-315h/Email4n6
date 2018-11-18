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
package com.github.email4n6;

import com.github.email4n6.model.Version;
import com.github.email4n6.utils.PathUtils;
import com.github.email4n6.view.StartupStage;
import com.github.email4n6.view.TabbedScene;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * This is the starting class.
 *
 * @author Marten4n6
 */
@Slf4j
public class Email4n6 extends Application {

    public static void main(String[] args) {
        log.info("Starting Email4n6 v{}...", Version.VERSION_NUMBER);

        launch(args);
    }

    @Override
    public void start(Stage stage) {
        confirmLiveInCurrentDirectory();
        createDirectories();

        // Show the main scene.
        stage.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
        stage.setScene(new TabbedScene().getScene());
        stage.centerOnScreen();
        stage.show();
        stage.setOnCloseRequest((event) -> {
            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Are you sure you want to quit?",
                    ButtonType.YES, ButtonType.NO
            );
            alert.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
            alert.setHeaderText(null);
            alert.initOwner(stage);
            alert.initModality(Modality.WINDOW_MODAL);

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.YES) {
                log.info("Shutting down...");

                Platform.exit();
                System.exit(0);
            } else {
                log.info("Shutdown cancelled.");
                event.consume();
            }
        });
    }

    private void confirmLiveInCurrentDirectory() {
        if (!Files.exists(Paths.get(PathUtils.getCasesPath()))) {
            StartupStage startupStage = new StartupStage();
            startupStage.showAndWait();

            if (!startupStage.isConfirmed()) {
                Platform.exit();
                System.exit(0);
            }
        }
    }

    private void createDirectories() {
        new File(PathUtils.getCasesPath()).mkdir();
        new File(PathUtils.getTempPath()).mkdir();
    }
}
