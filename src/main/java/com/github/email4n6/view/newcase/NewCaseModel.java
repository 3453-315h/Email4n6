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

import com.github.email4n6.model.casedao.CaseDAO;
import com.github.email4n6.model.casedao.JSONCaseDAO;
import com.github.email4n6.model.Settings;
import com.github.email4n6.utils.OSUtils;
import com.github.email4n6.model.casedao.Case;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;

/**
 * The model for the new case stage.
 */
@Slf4j
public class NewCaseModel {

    /**
     * @return A list of created case names.
     */
    public ArrayList<String> getCaseNames() {
        try {
            ArrayList<String> caseNames = new ArrayList<>();
            Path casesPath = Paths.get(OSUtils.getCasesPath());

            Files.list(casesPath).forEach(path -> caseNames.add(path.toFile().getName()));
            return caseNames;
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            return new ArrayList<>(0);
        }
    }

    /**
     * Creates the case.
     */
    public void createCase(Case createdCase) {
        new File(OSUtils.getCasePath(createdCase.getName())).mkdir();
        new File(OSUtils.getIndexPath(createdCase.getName())).mkdir();

        // Set default settings.
        Settings.set(createdCase.getName(), "DateFormat", "EEE, d MMM yyyy HH:mm:ss");
        Settings.set(createdCase.getName(), "SearchLimit", "100");

        // Persist case object to disk.
        CaseDAO caseDAO = new JSONCaseDAO();

        caseDAO.insertCase(createdCase);
    }
}
