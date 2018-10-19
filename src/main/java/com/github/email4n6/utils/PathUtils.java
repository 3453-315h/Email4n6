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

package com.github.email4n6.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Static class for paths related to this application.
 *
 * @author Marten4n6
 */
@Slf4j
public class PathUtils {

    private PathUtils() {
        throw new AssertionError("Don't.");
    }

    /**
     * @return The path to where Email4n6 lives (in the running JAR's directory).
     */
    private static String getApplicationPath() {
        try {
            return new File(PathUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent();
        } catch (URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
            return "";
        }
    }

    /**
     * @return The path where all case directories are stored.
     */
    public static String getCasesPath() {
        return getApplicationPath() + File.separator + "cases";
    }

    /**
     * @return The path to where all the case's data is stored.
     */
    public static String getCasePath(String caseName) {
        return getCasesPath() + File.separator + caseName;
    }

    /**
     * @return The path to the file where all case settings are stored.
     */
    public static File getSettingsFile(String caseName) {
        return new File(getCasePath(caseName) + File.separator + "settings.txt");
    }

    /**
     * @return The path where the case's index is stored.
     */
    public static String getIndexPath(String caseName) {
        return getCasePath(caseName) + File.separator + "index";
    }

    /**
     * @return The path where temporary files are stored.
     */
    public static String getTempPath() {
        return getApplicationPath() + File.separator + "temp";
    }
}
