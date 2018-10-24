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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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
		stage.setScene(new TabbedScene().getScene());
		stage.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
		stage.centerOnScreen();
		stage.show();
		stage.setOnCloseRequest((event) -> {
			log.info("Shutting down...");

			// TODO - Fire an event to shutdown gracefully (TreeTab executor).
			// TODO - Shutdown confirmation (with "don't ask again").

			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle("Shutdown confirmation");
			alert.setHeaderText(null);
			alert.setContentText("Confirm quit ?");
			ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
			ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
			alert.getButtonTypes().setAll(yesButton, noButton);
			Optional<ButtonType> result = alert.showAndWait();
			
			if(result.isPresent() ) {
				if(result.get() == yesButton) {
					log.info("Shutting down for real...");
					
					Platform.exit();
					System.exit(0);
				} else if (result.get() == noButton) {
					log.info("Not Shutting down ...");
					event.consume();
				}
			} else {
				log.info("Not Shutting down ...");
				
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
