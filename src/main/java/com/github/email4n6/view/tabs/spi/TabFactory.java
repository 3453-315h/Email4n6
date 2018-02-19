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

package com.github.email4n6.view.tabs.spi;

import com.github.email4n6.model.tagsdao.TagsDAO;
import com.github.email4n6.view.tabs.bookmarks.BookmarksController;
import com.github.email4n6.view.tabs.bookmarks.BookmarksModel;
import com.github.email4n6.view.tabs.bookmarks.BookmarksTab;
import com.github.email4n6.view.tabs.report.ReportController;
import com.github.email4n6.view.tabs.report.ReportModel;
import com.github.email4n6.view.tabs.report.ReportTab;
import com.github.email4n6.view.tabs.search.SearchController;
import com.github.email4n6.view.tabs.search.SearchTab;
import com.github.email4n6.view.tabs.tree.TreeController;
import com.github.email4n6.view.tabs.tree.TreeObject;
import com.github.email4n6.view.tabs.tree.TreeTab;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory which handles the creation of tabs.
 *
 * @author Marten4n6
 */
public class TabFactory {

    private List<GlobalTab> tabs = new ArrayList<>();

    public TabFactory(BookmarksModel bookmarksModel, TagsDAO tagsDAO, List<TreeItem<TreeObject>> createdTreeItems) {
        // Tabs
        TreeTab treeTab = new TreeTab();
        SearchTab searchTab = new SearchTab();
        BookmarksTab bookmarksTab = new BookmarksTab();
        ReportTab reportTab = new ReportTab();

        // Model
        ReportModel reportModel = new ReportModel(bookmarksModel, tagsDAO);

        // Tab controllers
        new TreeController(treeTab, createdTreeItems);
        new SearchController(searchTab);
        new BookmarksController(bookmarksTab, bookmarksModel);
        new ReportController(reportTab, reportModel);

        tabs.add(treeTab);
        tabs.add(searchTab);
        tabs.add(bookmarksTab);
        tabs.add(reportTab);
    }

    /**
     * @return A list of all tabs.
     */
    public List<GlobalTab> getTabs() {
        return tabs;
    }
}
