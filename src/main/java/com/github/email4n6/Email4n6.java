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
import com.github.email4n6.utils.OSUtils;
import com.github.email4n6.view.TabbedScene;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        stage.setScene(new TabbedScene().getScene());
        stage.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
        stage.centerOnScreen();
        stage.show();
    }

    private void confirmLiveInCurrentDirectory() {
        if (!Files.exists(Paths.get(OSUtils.getCasesPath()))) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "", ButtonType.YES, ButtonType.NO);
            Label content = new Label();

            content.setText("Email4n6 will live in the current folder, are you sure you want to continue?");
            content.setWrapText(true);

            alert.setTitle("Email4n6");
            alert.setHeaderText(null);
            alert.getDialogPane().setContent(content);
            alert.showAndWait();

            if (alert.getResult() == ButtonType.NO) {
                Platform.exit();
                System.exit(0);
            }
        }
    }

    private void createDirectories() {
        new File(OSUtils.getApplicationPath()).mkdir();
        new File(OSUtils.getCasesPath()).mkdir();
        new File(OSUtils.getTempPath()).mkdir();
    }
}
