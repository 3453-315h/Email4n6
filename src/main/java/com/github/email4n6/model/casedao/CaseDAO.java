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

import java.util.List;

/**
 * Case DAO interface.
 *
 * @author Marten4n6
 */
public interface CaseDAO {

    /**
     * @return A list of all created cases.
     */
    List<Case> findAll();

    /**
     * @return The case of the specified name.
     */
    Case findByName(String caseName);

    /**
     * Adds a case.
     *
     * @return True if the case was inserted successfully.
     */
    boolean insertCase(Case caseObject);

    /**
     * Updates a case.
     *
     * @return True if the case was updated successfully.
     */
    boolean updateCase(Case caseObject);

    /**
     * Deletes a case.
     *
     * @return True if the case was deleted successfully.
     */
    boolean deleteCase(String caseName);
}
