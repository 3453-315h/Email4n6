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

package com.github.email4n6.view.tabs.report;

import com.github.email4n6.model.casedao.Case;
import com.github.email4n6.message.factory.MessageFactory;
import com.github.email4n6.model.report.Report;
import com.github.email4n6.view.tabs.spi.GlobalTab;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

/**
 * Tab implementation for creating reports.
 *
 * @author Marten4n6
 */
@Slf4j
public class ReportTab implements GlobalTab {

    private Tab tab;
    private TextField fieldReportName;
    private TextField fieldOutputFolder;

    private @Getter Case currentCase;
    private @Getter MessageFactory messageFactory;
    private @Getter ComboBox<Report> comboBoxReportTypes;

    private @Setter EventHandler<Event> onTabSelection;
    private @Setter EventHandler<Event> onCreateReport;

    @Override
    public Tab getTab(Case currentCase, MessageFactory messageFactory) {
        this.currentCase = currentCase;
        this.messageFactory = messageFactory;

        tab = new Tab();

        // Tab
        tab.setText("Report");
        tab.setClosable(false);
        tab.setGraphic(new ImageView(new Image(this.getClass().getResourceAsStream("/images/report.png"))));

        // Listeners
        tab.setOnSelectionChanged((event) -> {
            if (tab.isSelected()) {
                onTabSelection.handle(event);
            }
        });

        return tab;
    }

    /**
     * Sets the bookmarks layout.
     */
    public void setBookmarksLayout(List<Report> reportTypes) {
        GridPane layout = new GridPane();

        // Layout
        layout.setPadding(new Insets(5, 5, 5, 5));
        layout.setHgap(5);
        layout.setVgap(5);

        // Labels
        Label labelReportType = new Label("Report type:");
        Label labelReportName = new Label("Report name:");
        Label labelOutputFolder = new Label("Output folder:");

        // Fields
        fieldReportName = new TextField();
        fieldOutputFolder = new TextField();

        fieldOutputFolder.setEditable(false);

        // Combo Box
        comboBoxReportTypes = new ComboBox<>(FXCollections.observableArrayList(reportTypes));

        comboBoxReportTypes.setConverter(new StringConverter<Report>() {
            @Override
            public String toString(Report report) {
                return report.getReportType();
            }

            @Override
            public Report fromString(String string) {
                for (Report report : comboBoxReportTypes.getItems()) {
                    if (report.getReportType().equals(report.getReportType())) {
                        return report;
                    }
                }
                return null;
            }
        });

        comboBoxReportTypes.getSelectionModel().selectFirst();
        comboBoxReportTypes.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(comboBoxReportTypes, Priority.ALWAYS);

        // Button
        Button buttonBrowseFolder = new Button(" ... ");
        Button buttonCreateReport = new Button("Create Report");

        buttonCreateReport.setPrefWidth(200);
        buttonCreateReport.setMinWidth(200);
        buttonCreateReport.setMinHeight(30);

        // HBox
        HBox outputHBox = new HBox();

        outputHBox.getChildren().addAll(fieldOutputFolder, buttonBrowseFolder);
        outputHBox.setSpacing(5);

        fieldOutputFolder.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(fieldOutputFolder, Priority.ALWAYS);

        // Add
        GridPane settingsPane = comboBoxReportTypes.getSelectionModel().getSelectedItem().getSettingsPane();

        if (settingsPane != null) {
            GridPane.setHgrow(settingsPane, Priority.ALWAYS);
            GridPane.setColumnSpan(settingsPane, GridPane.REMAINING);

            layout.addRow(0, labelReportType, comboBoxReportTypes);
            layout.addRow(1, labelReportName, fieldReportName);
            layout.addRow(2, labelOutputFolder, outputHBox);
            layout.addRow(4, createSeparator());
            layout.addRow(6, settingsPane);
            layout.addRow(8, createSeparator());
            layout.addRow(10, new Label(), buttonCreateReport);
        } else {
            layout.addRow(0, labelReportType, comboBoxReportTypes);
            layout.addRow(1, labelReportName, fieldReportName);
            layout.addRow(2, labelOutputFolder, outputHBox);
            layout.addRow(3, new Label(), buttonCreateReport);
        }

        // Listeners
        buttonBrowseFolder.setOnAction((event) -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(layout.getScene().getWindow());

            if (selectedDirectory != null) {
                fieldOutputFolder.setText(selectedDirectory.getPath());
            }
        });
        buttonCreateReport.setOnAction((event) -> {
            if (fieldReportName.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Invalid report name.", ButtonType.CLOSE).showAndWait();
            } else if (fieldOutputFolder.getText().trim().isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Invalid output folder.", ButtonType.CLOSE).showAndWait();
            } else {
                onCreateReport.handle(event);
            }
        });

        tab.setContent(layout);
    }

    /**
     * Sets the no bookmarks layout.
     */
    public void setNoBookmarksLayout() {
        GridPane layout = new GridPane();

        layout.setPadding(new Insets(5, 5, 5, 5));
        layout.addRow(0, new Label("Bookmarks are required to create a report."));

        tab.setContent(layout);
    }

    private HBox createSeparator() {
        HBox hBox = new HBox();
        Separator separator = new Separator();

        hBox.getChildren().add(separator);
        hBox.setAlignment(Pos.CENTER);

        HBox.setHgrow(separator, Priority.ALWAYS);
        GridPane.setColumnSpan(hBox, GridPane.REMAINING);

        separator.setMaxWidth(Double.MAX_VALUE);
        return hBox;
    }

    /**
     * @return The report name.
     */
    public String getReportName() {
        return fieldReportName.getText();
    }

    /**
     * @return The output folder.
     */
    public String getOutputFolder() {
        return fieldOutputFolder.getText();
    }
}
