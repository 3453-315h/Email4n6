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

import java.io.File;

import com.github.email4n6.model.report.Report;
import com.github.email4n6.model.report.ReportConfiguration;

import javafx.event.Event;
import javafx.event.EventHandler;

/**
 * Controls the report tab.
 *
 * @author Marten4n6
 */
public class ReportController {

    private ReportTab reportTab;
    private ReportModel reportModel;

    public ReportController(ReportTab reportTab, ReportModel reportModel) {
        this.reportTab = reportTab;
        this.reportModel = reportModel;

        // Catch events fired by the report tab.
        reportTab.setOnTabSelection(new TabSelectionListener());
        reportTab.setOnCreateReport(new CreateReportListener());
    }

    /**
     * Handles tab selection events.
     */
    class TabSelectionListener implements EventHandler<Event> {

        @Override
        public void handle(Event event) {
            if (reportModel.getBookmarksModel().hasBookmarks()) {
                reportTab.setBookmarksLayout(reportModel.getReportTypes());
            } else {
                reportTab.setNoBookmarksLayout();
            }
        }
    }

    /**
     * Handles create report events.
     */
    class CreateReportListener implements EventHandler<Event> {

        @Override
        public void handle(Event event) {
            Report selectedReport = reportTab.getComboBoxReportTypes().getSelectionModel().getSelectedItem();

            ReportConfiguration configuration = ReportConfiguration.builder()
                    .currentCase(reportModel.getCurrentCase())
                    .reportName(reportTab.getReportName())
                    .outputFolder(new File(reportTab.getOutputFolder()))
                    .bookmarksModel(reportModel.getBookmarksModel())
                    .tagsDAO(reportModel.getTagModel())
                    .messageFactory(reportModel.getMessageFactory()).build();

            selectedReport.createReport(configuration);
        }
    }
}
