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

package com.github.email4n6.model;

import com.github.email4n6.utils.OSUtils;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Static class which handles settings.
 *
 * @author Marten4n6
 */
@Slf4j
public final class Settings {

    private Settings() {
        throw new AssertionError("Don't");
    }

    /**
     * Sets the value of the key.
     */
    public static void set(String caseName, String key, String value) {
        try {
            Properties properties = new Properties();

            if (OSUtils.getSettingsFile(caseName).exists()) {
                // Load existing settings.
                @Cleanup FileInputStream inputStream = new FileInputStream(OSUtils.getSettingsFile(caseName));
                properties.load(inputStream);
            }

            @Cleanup FileOutputStream outputStream = new FileOutputStream(OSUtils.getSettingsFile(caseName));

            properties.put(key, value);
            properties.store(outputStream, null);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * @return The value of the key otherwise an empty string.
     */
    public static String get(String caseName, String key) {
        try {
            Properties properties = new Properties();
            @Cleanup FileInputStream inputStream = new FileInputStream(OSUtils.getSettingsFile(caseName));

            properties.load(inputStream);
            return properties.get(key).toString();
        } catch (IOException | NullPointerException ex) {
            return "";
        }
    }
}
