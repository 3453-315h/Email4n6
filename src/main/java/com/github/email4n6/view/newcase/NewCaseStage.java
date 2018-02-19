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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * Stage for creating new cases.
 *
 * @author Marten4n6
 */
public class NewCaseStage {

    private Stage stage;
    private @Getter Window ownerWindow;
    private @Getter Set<String> sources;
    private @Getter @Setter Case createdCase;

    private TextField fieldName;
    private TextField fieldDescription;
    private TextField fieldInvestigator;
    private TextField fieldSources;

    private ComboBox<String> comboBoxSourceType;
    private CheckBox checkBoxSubFolders;

    private @Setter EventHandler<ActionEvent> onBrowseSource;
    private @Setter EventHandler<ActionEvent> onCreateCase;

    public NewCaseStage(Window ownerWindow) {
        this.ownerWindow = ownerWindow;
        this.sources = new HashSet<>();

        stage = new Stage();
        BorderPane sceneLayout = new BorderPane();
        Scene scene = new Scene(sceneLayout, 500, 250);

        TabPane tabPane = new TabPane();
        tabPane.getTabs().addAll(createCaseTab());

        sceneLayout.setCenter(tabPane);

        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(ownerWindow);
        stage.setResizable(false);
        stage.setTitle("Email4n6");
        stage.setScene(scene);
    }

    private Tab createCaseTab() {
        Tab tab = new Tab();
        GridPane gridPane = new GridPane();

        // Tab
        tab.setText("Case");
        tab.setClosable(false);
        tab.setContent(gridPane);

        // GridPane
        gridPane.setPadding(new Insets(5, 5, 5, 5));
        gridPane.setHgap(5);
        gridPane.setVgap(5);

        ColumnConstraints columnOneConstraints = new ColumnConstraints();
        ColumnConstraints columnTwoConstraints = new ColumnConstraints();

        columnOneConstraints.setMinWidth(80);
        columnTwoConstraints.setMinWidth(250);

        gridPane.getColumnConstraints().addAll(columnOneConstraints, columnTwoConstraints);

        // Labels
        Label labelName = new Label("Name:");
        Label labelDescription = new Label("Description:");
        Label labelInvestigator = new Label("Investigator:");
        Label labelSource = new Label("Source(s):");

        // Fields
        fieldName = new TextField();
        fieldDescription = new TextField();
        fieldInvestigator = new TextField();
        fieldSources = new TextField();

        fieldInvestigator.setText(System.getProperty("user.name"));
        fieldSources.setEditable(false);

        // Buttons
        Button buttonCreateCase = new Button("Create Case");
        Button buttonBrowseSource = new Button(" ... ");

        buttonCreateCase.setMaxWidth(Double.MAX_VALUE);
        buttonCreateCase.setMinHeight(40);

        // Check Boxes
        HBox hBox = new HBox();
        checkBoxSubFolders = new CheckBox();

        checkBoxSubFolders.setText("SubFolders");
        checkBoxSubFolders.setSelected(true);
        checkBoxSubFolders.setVisible(false);
        checkBoxSubFolders.setManaged(false);

        hBox.setSpacing(10);
        hBox.getChildren().add(checkBoxSubFolders);

        // Combo Box
        comboBoxSourceType = new ComboBox<>();

        comboBoxSourceType.getItems().addAll("File(s)", "Folder");
        comboBoxSourceType.setValue("File(s)");

        // Add
        gridPane.addRow(0, labelName, fieldName);
        gridPane.addRow(1, labelDescription, fieldDescription);
        gridPane.addRow(2, labelInvestigator, fieldInvestigator);
        gridPane.addRow(3, labelSource, fieldSources, buttonBrowseSource, comboBoxSourceType);
        gridPane.addRow(4, new Label(), buttonCreateCase);
        gridPane.addRow(5, new Label(), hBox);

        // Listeners
        comboBoxSourceType.setOnAction((event) -> {
            if (comboBoxSourceType.getSelectionModel().getSelectedItem().startsWith("File")) {
                checkBoxSubFolders.setVisible(false);
                checkBoxSubFolders.setManaged(false);
            } else {
                checkBoxSubFolders.setVisible(true);
                checkBoxSubFolders.setManaged(true);
            }
        });
        buttonBrowseSource.setOnAction((event) -> onBrowseSource.handle(event));
        buttonCreateCase.setOnAction((event) -> onCreateCase.handle(event));

        return tab;
    }

    /**
     * Shows the new case stage.
     */
    public void showAndWait() {
        stage.centerOnScreen();
        stage.showAndWait();
    }

    /**
     * Closes the new case stage.
     */
    public void close() {
        stage.close();
    }

    /**
     * Alerts the user that an error occurred.
     *
     * @param message The error message displayed to the user.
     */
    public void displayError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.CLOSE);

        alert.setTitle("Email4n6");
        alert.setHeaderText(null);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.initOwner(stage);

        alert.show();
    }

    /**
     * @return The name of the created case.
     */
    public String getName() {
        return fieldName.getText();
    }

    /**
     * @return The description of the created case.
     */
    public String getDescription() {
        return fieldDescription.getText();
    }

    /**
     * @return The investigator of the created case.
     */
    public String getInvestigator() {
        return fieldInvestigator.getText();
    }

    /**
     * @return The selected source type.
     */
    public String getSourceType() {
        return comboBoxSourceType.getSelectionModel().getSelectedItem();
    }

    /**
     * Sets the source text.
     */
    public void setSourceText(String text) {
        fieldSources.setText(text);
    }

    /**
     * @return True if subfolders should be parsed.
     */
    public boolean parseSubFolders() {
        return checkBoxSubFolders.isSelected();
    }
}
