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

package com.github.email4n6.view.home;

import com.github.email4n6.model.casedao.Case;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import lombok.Getter;
import lombok.Setter;

import java.util.Optional;

/**
 * This is the home tab.
 *
 * @author Marten4n6
 */
public class HomeTab {

    private @Getter Tab tab;
    private @Getter TableView<Case> table;

    private @Setter EventHandler<ActionEvent> onOpenCase;
    private @Setter EventHandler<ActionEvent> onNewCase;
    private @Setter EventHandler<ActionEvent> onRemoveCase;

    public HomeTab() {
        tab = new Tab();
        BorderPane tabLayout = new BorderPane();

        tabLayout.setPadding(new Insets(5, 5, 0, 0));
        tabLayout.setCenter(createCaseTable());
        tabLayout.setRight(createButtonsVBox());

        tab.setText("Home");
        tab.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/home.png"))));
        tab.setClosable(false);
        tab.setContent(tabLayout);
    }

    private TableView<Case> createCaseTable() {
        table = new TableView<>();

        TableColumn<Case, String> columnName = new TableColumn<>("Name");
        TableColumn<Case, String> columnDescription = new TableColumn<>("Description");
        TableColumn<Case, String> columnInvestigator = new TableColumn<>("Investigator");
        TableColumn<Case, String> columnSize = new TableColumn<>("Size");

        columnName.setCellValueFactory(new PropertyValueFactory<>("name"));
        columnDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        columnInvestigator.setCellValueFactory(new PropertyValueFactory<>("investigator"));
        columnSize.setCellValueFactory(new PropertyValueFactory<>("size"));

        table.setPlaceholder(new Label("No cases created."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getColumns().add(columnName);
        table.getColumns().add(columnDescription);
        table.getColumns().add(columnInvestigator);
        table.getColumns().add(columnSize);

        return table;
    }

    private VBox createButtonsVBox() {
        VBox vBox = new VBox();

        Button newCase = new Button("New Case");
        Button openCase = new Button("Open Case");
        Button removeCase = new Button("Remove Case");

        vBox.setSpacing(10);
        vBox.setPadding(new Insets(0, 0, 0, 5));

        newCase.setMinWidth(185);
        openCase.setMinWidth(185);
        removeCase.setMinWidth(185);

        vBox.getChildren().addAll(newCase, openCase, removeCase);

        // Listeners
        newCase.setOnAction((event) -> onNewCase.handle(event));
        openCase.setOnAction((event) -> onOpenCase.handle(event));
        removeCase.setOnAction((event) -> onRemoveCase.handle(event));

        return vBox;
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
        alert.initOwner(table.getScene().getWindow());

        alert.show();
    }

    /**
     * Shows a confirmation alert to the user.
     *
     * @return The (optional) ButtonType clicked.
     */
    public Optional<ButtonType> displayConfirmation(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);

        alert.setTitle("Email4n6");
        alert.setHeaderText(null);
        alert.initModality(Modality.WINDOW_MODAL);
        alert.initOwner(table.getScene().getWindow());

        return alert.showAndWait();
    }
}
