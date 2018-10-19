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

import com.github.email4n6.model.Case;
import com.github.email4n6.model.message.factory.MessageFactory;
import com.github.email4n6.model.report.HTMLReport;
import com.github.email4n6.model.report.Report;
import com.github.email4n6.view.tabs.bookmarks.BookmarksModel;
import com.github.email4n6.view.tabs.bookmarks.TagModel;
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
    private @Getter TagModel tagModel;
    private @Getter MessageFactory messageFactory;
    private @Getter Case currentCase;

    private @Getter List<Report> reportTypes = new ArrayList<>();

    public ReportModel(BookmarksModel bookmarksModel, TagModel tagModel, MessageFactory messageFactory, Case currentCase) {
        this.bookmarksModel = bookmarksModel;
        this.tagModel = tagModel;
        this.messageFactory = messageFactory;
        this.currentCase = currentCase;

        // Add all report types here...
        reportTypes.add(new HTMLReport());
    }
}
