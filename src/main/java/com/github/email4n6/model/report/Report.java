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

import javafx.scene.layout.GridPane;

/**
 * Interface for creating reports.
 *
 * @author Marten4n6
 */
public interface Report {

    /**
     * @return The type of report the implementation will create.
     */
    String getReportType();

    /**
     * Creates a report.
     */
    void createReport(ReportConfiguration configuration);

    /**
     * @return An optional pane which is shown when creating a report.
     */
    GridPane getSettingsPane();
}
