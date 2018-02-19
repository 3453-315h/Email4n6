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

package com.github.email4n6.model.casedao;

import com.github.email4n6.utils.OSUtils;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * JSON case DAO implementation.
 *
 * @author Marten4n6
 */
@Slf4j
public class JSONCaseDAO implements CaseDAO {

    private Gson gson;

    public JSONCaseDAO() {
        gson = new Gson();
    }

    @Override
    public List<Case> findAll() {
        List<Case> cases = new ArrayList<>();

        getCaseNames().forEach(caseName -> {
            cases.add(getCase(caseName));
        });
        return cases;
    }

    @Override
    public Case findByName(String caseName) {
        return getCase(caseName);
    }

    /**
     * @return A list of created case names.
     */
    private ArrayList<String> getCaseNames() {
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
     * @return The case object of the specified case.
     */
    private Case getCase(String caseName) {
        try {
            Path casePath = Paths.get(getCaseObjectPath(caseName));

            return gson.fromJson(Files.newBufferedReader(casePath).readLine(), Case.class);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * @return The path to where the case object is stored.
     */
    private String getCaseObjectPath(String caseName) {
        return OSUtils.getCasePath(caseName) + File.separator + "Case.json";
    }

    @Override
    public boolean insertCase(Case createdCase) {
        try {
            Files.write(
                    Paths.get(getCaseObjectPath(createdCase.getName())),
                    gson.toJson(createdCase).getBytes()
            );
            return true;
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            return false;
        }
    }

    @Override
    public boolean updateCase(Case caseObject) {
        // TODO -
        return false;
    }

    @Override
    public boolean deleteCase(String caseName) {
        try {
            Files.walk(Paths.get(OSUtils.getCasePath(caseName)))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            return true;
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            return false;
        }
    }
}
