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
package com.github.email4n6.view.tabs.home;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.email4n6.model.Case;
import com.github.email4n6.model.Version;
import com.github.email4n6.utils.PathUtils;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Getter;
import lombok.Setter;

/**
 * "Dumb" class, only handles what the user sees and fires events.
 *
 * @author Marten4n6
 */
public class HomeTab {

    private @Getter Tab tab;
    private @Getter TableView<Case> table;

    private @Setter EventHandler<ActionEvent> onCreateCase;
    private @Setter EventHandler<ActionEvent> onOpenCase;
    private @Setter EventHandler<ActionEvent> onRemoveCase;

    /**
     * Initializes the home tab.
     */
    public HomeTab() {
        tab = new Tab();
        BorderPane tabLayout = new BorderPane();
        table = createCaseTable();

        tabLayout.setPadding(new Insets(5, 5, 0, 0));
        tabLayout.setCenter(table);
        tabLayout.setRight(createButtonsVBox());

        tab.setText("Home");
        tab.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/home.png"))));
        tab.setClosable(false);
        tab.setContent(tabLayout);
    }

    /**
     * Creates the case table.
     */
    private TableView<Case> createCaseTable() {
        TableView<Case> table = new TableView<>();
        
        TableColumn<Case, String> columnId = new TableColumn<>("#");
        TableColumn<Case, String> columnName = new TableColumn<>("Name");
        TableColumn<Case, String> columnDescription = new TableColumn<>("Description");
        TableColumn<Case, String> columnInvestigator = new TableColumn<>("Investigator");
        TableColumn<Case, String> columnSize = new TableColumn<>("Size");

        columnId.setCellValueFactory(new PropertyValueFactory<>("id"));
        columnId.setMinWidth(50);
        columnId.setMaxWidth(100);
        columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        columnInvestigator.setCellValueFactory(new PropertyValueFactory<>("investigator"));
        columnSize.setCellValueFactory(new PropertyValueFactory<>("size"));

        table.setPlaceholder(new Label("No cases have been created."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        table.getColumns().add(columnId);
        table.getColumns().add(columnName);
        table.getColumns().add(columnDescription);
        table.getColumns().add(columnInvestigator);
        table.getColumns().add(columnSize);

        return table;
    }

    /**
     * Creates the right layout of buttons.
     */
    private VBox createButtonsVBox() {
        VBox vBox = new VBox();

        Button buttonNewCase = new Button("Create");
        Button buttonOpenCase = new Button("Open");
        Button buttonRemoveCase = new Button("Remove");

        buttonNewCase.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/add.png"))));
        buttonNewCase.setMinWidth(185);
        buttonNewCase.setAlignment(Pos.CENTER_LEFT);

        buttonOpenCase.setMinWidth(185);
        buttonOpenCase.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/open.png"))));
        buttonOpenCase.setAlignment(Pos.CENTER_LEFT);
        buttonOpenCase.setDefaultButton(true);

        buttonRemoveCase.setMinWidth(185);
        buttonRemoveCase.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/trash.png"))));
        buttonRemoveCase.setAlignment(Pos.CENTER_LEFT);

        // Logo
        Label labelLogo = new Label();
        Label labelSeparator = new Label();

        labelLogo.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/logo-home.png"))));

        labelSeparator.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(labelSeparator, Priority.ALWAYS);

        // Add
        vBox.getChildren().addAll(buttonNewCase, buttonOpenCase, buttonRemoveCase, labelSeparator, labelLogo);
        vBox.setSpacing(10);
        vBox.setPadding(new Insets(25, 0, 0, 5));

        // Listeners
        buttonNewCase.setOnAction((event) -> onCreateCase.handle(event));
        buttonOpenCase.setOnAction((event) -> onOpenCase.handle(event));
        buttonRemoveCase.setOnAction((event) -> onRemoveCase.handle(event));

        return vBox;
    }

    /**
     * Alerts the user that an error occurred.
     *
     * @param message The error message displayed to the user.
     */
    void displayError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.CLOSE);

        alert.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
        alert.setHeaderText(null);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.initOwner(table.getScene().getWindow());

        alert.show();
    }

    /**
     * Shows a confirmation alert to the user.
     *
     * @return The (optional) ButtonType clicked.
     */
    Optional<ButtonType> displayConfirmation(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);

        alert.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
        alert.setHeaderText(null);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.initOwner(table.getScene().getWindow());

        return alert.showAndWait();
    }

    /**
     * Subclass for creating cases.
     */
    class CreateCaseStage {

        private Stage stage;
        private @Getter Case createdCase;

        private @Setter EventHandler<ActionEvent> onCreateCase;

        /**
         * Initializes the create case stage.
         */
        CreateCaseStage(Window ownerWindow) {
            stage = new Stage();

            GridPane gridPane = new GridPane();
            Scene scene = new Scene(gridPane, 500, 220);

            Set<String> sources = new HashSet<>();

            // GridPane
            gridPane.setPadding(new Insets(10, 5, 5, 5));
            gridPane.setHgap(5);
            gridPane.setVgap(5);

            // Labels
            Label labelName = new Label("Name:");
            Label labelDescription = new Label("Description:");
            Label labelInvestigator = new Label("Investigator:");
            Label labelSource = new Label("Source(s):");

            // Fields
            TextField fieldName = new TextField();
            TextField fieldDescription = new TextField();
            TextField fieldInvestigator = new TextField();
            TextField fieldSources = new TextField();

            GridPane.setHgrow(fieldName, Priority.ALWAYS);
            fieldInvestigator.setText(System.getProperty("user.name"));
            fieldSources.setEditable(false);

            // Buttons
            HBox buttonsLayout = new HBox();
            Button buttonCreate = new Button("Create");
            Button buttonCancel = new Button("Cancel");
            Button buttonBrowseSource = new Button(" ... ");

            buttonCreate.setDefaultButton(true);
            buttonCreate.setMinWidth(120);
            buttonCancel.setMinWidth(120);

            GridPane.setMargin(buttonsLayout, new Insets(15, 0, 0, 0));
            buttonsLayout.getChildren().addAll(buttonCancel, buttonCreate);
            buttonsLayout.setSpacing(5);

            // Checkboxes
            HBox checkboxesLayout = new HBox();
            CheckBox checkBoxSubFolders = new CheckBox();

            checkBoxSubFolders.setText("Subfolders");
            checkBoxSubFolders.setSelected(true);
            checkBoxSubFolders.setVisible(false);
            checkBoxSubFolders.setManaged(false);

            checkboxesLayout.setSpacing(10);
            checkboxesLayout.getChildren().add(checkBoxSubFolders);

            // ComboBox
            ComboBox<String> comboBoxSourceType = new ComboBox<>();

            comboBoxSourceType.getItems().addAll("File(s)", "Folder");
            comboBoxSourceType.getSelectionModel().selectFirst();

            // Add
            gridPane.addRow(0, labelName, fieldName);
            gridPane.addRow(1, labelDescription, fieldDescription);
            gridPane.addRow(2, labelInvestigator, fieldInvestigator);
            gridPane.addRow(3, labelSource, fieldSources, buttonBrowseSource, comboBoxSourceType);
            gridPane.addRow(4, new Label(), buttonsLayout);
            gridPane.addRow(5, new Label(), checkboxesLayout);

            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(ownerWindow);
            stage.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
            stage.setScene(scene);

            // Listeners
            comboBoxSourceType.setOnAction((event) -> {
                if (comboBoxSourceType.getSelectionModel().getSelectedItem().equals("File(s)")) {
                    checkBoxSubFolders.setVisible(false);
                    checkBoxSubFolders.setManaged(false);
                } else {
                    checkBoxSubFolders.setVisible(true);
                    checkBoxSubFolders.setManaged(true);
                }
            });
            buttonBrowseSource.setOnAction((event) -> {
                if (comboBoxSourceType.getSelectionModel().getSelectedItem().equals("File(s)")) {
                    FileChooser fileChooser = new FileChooser();

                    // Extension filter
                    // TODO - Dynamically update the list of extensions.
                    FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(
                            "Supported .PST or .OST files",
                            Arrays.asList("*.pst", "*.ost")
                    );
                    fileChooser.getExtensionFilters().add(filter);

                    List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage.getScene().getWindow());

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

                        sources.clear();
                        selectedFiles.forEach(file -> sources.add(file.getPath()));

                        fieldSources.setText(sourceText.toString());
                    }
                } else {
                    DirectoryChooser directoryChooser = new DirectoryChooser();
                    File selectedDirectory = directoryChooser.showDialog(stage.getScene().getWindow());

                    if (selectedDirectory != null) {
                        sources.clear();
                        sources.add(selectedDirectory.getPath());

                        fieldSources.setText(selectedDirectory.getName());
                    }
                }
            });
            buttonCreate.setOnAction((event) -> {
                if (fieldName.getText().trim().isEmpty()) {
                    displayError("Please specify a valid case name.");
                } else if (fieldInvestigator.getText().trim().isEmpty()) {
                    displayError("Please specify a valid investigator.");
                } else if (fieldSources.getText().trim().isEmpty()) {
                    displayError("Please specify a valid source.");
                } else {
                    createdCase = Case.builder()
                    		.id(PathUtils.getNumberOfCases() + 1)
                            .name(fieldName.getText())
                            .investigator(fieldInvestigator.getText())
                            .description(fieldDescription.getText())
                            .size("Not calculated yet.")
                            .sources(sources).build();

                    onCreateCase.handle(event);
                }
            });
            buttonCancel.setOnAction((event) -> stage.close());
        }

        /**
         * Alerts the user that an error occurred.
         *
         * @param message The error message displayed to the user.
         */
        void displayError(String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.CLOSE);

            alert.setTitle("Email4n6 v" + Version.VERSION_NUMBER);
            alert.setHeaderText(null);
            alert.initModality(Modality.WINDOW_MODAL);
            alert.initOwner(stage.getScene().getWindow());

            alert.show();
        }

        void show() {
            stage.centerOnScreen();
            stage.show();
        }

        void close() {
            stage.close();
        }
    }
}
