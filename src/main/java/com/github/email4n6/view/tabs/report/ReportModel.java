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

import com.github.email4n6.model.report.HTMLReport;
import com.github.email4n6.model.report.Report;
import com.github.email4n6.model.tagsdao.TagsDAO;
import com.github.email4n6.view.tabs.bookmarks.BookmarksModel;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for the report tab.
 *
 * @author Marten4n6
 */
public class ReportModel {

    private @Getter BookmarksModel bookmarksModel;
    private @Getter TagsDAO tagsDAO;

    private @Getter List<Report> reportTypes = new ArrayList<>();

    public ReportModel(BookmarksModel bookmarksModel, TagsDAO tagsDAO) {
        this.bookmarksModel = bookmarksModel;
        this.tagsDAO = tagsDAO;

        // Add all report types
        reportTypes.add(new HTMLReport());
    }


}
