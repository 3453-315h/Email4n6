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

package com.github.email4n6.model.report;

import com.github.email4n6.model.message.MessageRow;
import com.github.email4n6.model.message.MessageValue;
import com.github.email4n6.model.Settings;
import com.github.email4n6.model.Case;
import freemarker.template.*;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * Report implementation which generates HTML reports (based on X-Ways).
 *
 * @author Marten4n6
 */
@Slf4j
public class HTMLReport implements Report {

    private Spinner<Integer> spinner;
    private ComboBox<Integer> comboBox;

    @Override
    public String getReportType() {
        return "HTML";
    }

    @Override
    public void createReport(ReportConfiguration configuration) {
        log.info("Creating HTML report...");

        // Check if there's not already a report with this name.
        if (new File(configuration.getOutputFolder().getPath() + File.separator + configuration.getReportName() + "_1.html").exists()) {
            log.warn("A report with that name already exists, stopping...");
            new Alert(Alert.AlertType.ERROR, "A report with that name already exists.", ButtonType.CLOSE).showAndWait();
            return;
        }

        List<String> bookmarks = configuration.getBookmarksModel().getBookmarks();
        List<MessageRow> pageItems = new ArrayList<>(); // The items of the current page.

        int pageItemCount = 1;
        int totalItemCount = 1;
        int pageNumber = 0;
        int maxItemsPerPage = spinner.getValue();

        log.debug("Total bookmarks: {}", bookmarks.size());
        log.debug("Maximum items per page: {}", maxItemsPerPage);

        for (String bookmarkID : bookmarks) {
            pageItems.add(configuration.getMessageFactory().getMessageRow(bookmarkID));

            // Create the HTML version of this message and
            // outputs all attachments.
            createMessage(bookmarkID, configuration);

            if (bookmarks.size() == totalItemCount) { // There's no more items left, create page.
                log.debug("No more items left, creating page!");
                pageNumber++;

                createPage(configuration, pageItems, pageNumber, false, comboBox.getValue());
                pageItems.clear();
            } else if (pageItemCount == maxItemsPerPage) { // Maximum items (per page) reached, create page.
                log.debug("Maximum items reached, creating page!");
                pageItemCount = 0;
                pageNumber++;

                createPage(configuration, pageItems, pageNumber, true, comboBox.getValue());
                pageItems.clear();
            }

            pageItemCount++;
            totalItemCount++;
        }

        try {
            Files.copy(
                    HTMLReport.class.getClassLoader().getResourceAsStream("freemarker/Report.css"),
                    Paths.get(configuration.getOutputFolder().getPath() + File.separator + "Report.css")
            );
        } catch (FileAlreadyExistsException ex) {
            // CSS file already exists.
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

        log.info("Report created.");

        Optional<ButtonType> confirmOpen = new Alert(Alert.AlertType.CONFIRMATION, "Report created, would you like to open the report?", ButtonType.YES, ButtonType.NO).showAndWait();

        if (confirmOpen.isPresent() && confirmOpen.get() == ButtonType.YES) {
            SwingUtilities.invokeLater(() -> {
                try {
                    Desktop.getDesktop().open(new File(configuration.getOutputFolder().getPath() + File.separator + configuration.getReportName() + "_1.html"));
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            });
        }
    }

    /**
     * Creates the HTML version of the message.
     */
    private void createMessage(String messageID, ReportConfiguration configuration) {
        Map<String, Object> modelAndView = new HashMap<>();

        File messageFolder = new File(configuration.getOutputFolder().getPath() + File.separator + "Files_" + configuration.getReportName());
        File messageFile = new File(messageFolder.getPath() + File.separator + messageID + ".html");

        messageFolder.mkdir();

        MessageRow messageRow = configuration.getMessageFactory().getMessageRow(messageID);
        MessageValue messageValue = configuration.getMessageFactory().getMessageValue(messageID);

        // Output attachments
        messageValue.getAttachments().forEach(attachment -> {
            try {
                Files.copy(
                        attachment.getInputStream(),
                        Paths.get(messageFolder + File.separator + (messageRow.getId() + "-" + attachment.getAttachmentName()))
                );
            } catch (IOException ex) {
                log.error("Failed to copy attachment.", ex);
            }
        });

        modelAndView.put("report_name", configuration.getReportName());
        modelAndView.put("message_row", messageRow);
        modelAndView.put("message_value", messageValue);
        outputToHTML(configuration.getCurrentCase(), modelAndView, "HTMLMessage.html", messageFile);
    }

    /**
     * Creates a page of items.
     *
     * @param items The items on this pages.
     */
    private void createPage(ReportConfiguration configuration, List<MessageRow> items, int pageNumber, boolean hasNextPage, int columns) {
        Map<String, Object> modelAndView = new HashMap<>();
        File outputFile = new File(configuration.getOutputFolder().getPath() + File.separator + configuration.getReportName() + "_" + pageNumber + ".html");

        modelAndView.put("case", configuration.getCurrentCase());
        modelAndView.put("items", items);
        modelAndView.put("report_name", configuration.getReportName());
        modelAndView.put("page_number", pageNumber);
        modelAndView.put("has_next_page", hasNextPage);
        modelAndView.put("columns", columns);

        outputToHTML(configuration.getCurrentCase(), modelAndView, "HTMLReport.html", outputFile);
    }

    /**
     * Renders the modelAndView to HTML with freemarker.
     *
     * @param currentCase  The currently open case.
     * @param modelAndView The model and view.
     * @param templateName The name of the template to use.
     * @param outputFile   The file to output to.
     */
    private void outputToHTML(Case currentCase, Map<String, Object> modelAndView, String templateName, File outputFile) {
        try {
            Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);

            configuration.setClassForTemplateLoading(HTMLReport.class, "/freemarker");
            configuration.setIncompatibleImprovements(new Version(2, 3, 23));
            configuration.setDefaultEncoding("UTF-8");
            configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            configuration.setSetting("date_format", Settings.get(currentCase.getName(), "date_format"));

            Template template = configuration.getTemplate(templateName);
            Writer fileWriter = new FileWriter(outputFile);

            template.process(modelAndView, fileWriter);
            fileWriter.close();
        } catch (IOException | TemplateException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override
    public GridPane getSettingsPane() {
        GridPane settingsPane = new GridPane();

        // Layout
        settingsPane.setHgap(5);
        settingsPane.setVgap(5);

        // Labels
        Label labelItemsPerPage = new Label("Items per page:");
        Label labelColumnsPerPage = new Label("Columns:");

        // Spinner (items per page)
        final int SPINNER_MAX = 500;
        final int SPINNER_DEFAULT = 200;

        spinner = new Spinner<>(1, 500, 100);

        spinner.setEditable(true);
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, SPINNER_MAX, SPINNER_DEFAULT, 10));

        spinner.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(spinner, Priority.ALWAYS);

        // Combo box (columns per page)
        comboBox = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4, 5));

        comboBox.setValue(3);
        comboBox.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(comboBox, Priority.ALWAYS);

        // Add
        settingsPane.addRow(0, labelItemsPerPage, spinner);
        settingsPane.addRow(1, labelColumnsPerPage, comboBox);

        // Listeners
        spinner.getEditor().addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                try {
                    // Make sure the user entered a valid number and
                    // that it is not greater than SPINNER_MAX.
                    int enteredAmount = Integer.parseInt(spinner.getEditor().textProperty().get());

                    if (enteredAmount > SPINNER_MAX) {
                        spinner.getEditor().textProperty().set("" + SPINNER_MAX);
                    }
                } catch (NumberFormatException ex) {
                    spinner.getEditor().textProperty().set("" + SPINNER_DEFAULT);
                }
            }
        });

        return settingsPane;
    }
}
