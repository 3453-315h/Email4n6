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

package com.github.email4n6.parser.spi;

import com.github.email4n6.parser.impl.pst.PSTParser;
import com.github.email4n6.parser.spi.Parser;
import com.github.email4n6.message.MessageRow;
import com.github.email4n6.message.MessageValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Factory for creating parsers.
 *
 * @author Marten4n6
 */
public class ParserFactory {

    private final List<Parser> parsers = new ArrayList<>();

    public ParserFactory() {
        // Add all parsers here...
        parsers.add(new PSTParser());
    }

    /**
     * @return A list of all parsers.
     */
    public List<Parser> getParsers() {
        return parsers;
    }

    /**
     * @return A list of parsers that accept the specified extension.
     */
    public List<Parser> getParsers(String extension) {
        List<Parser> supportedParsers = new ArrayList<>();

        parsers.forEach(parser -> {
            if (parser.getSupportedFileExtensions().contains(extension)) {
                supportedParsers.add(parser);
            }
        });
        return supportedParsers;
    }

    /**
     * @return A set of all supported file extensions.
     */
    public Set<String> getAllSupportedFileExtensions() {
        Set<String> extensions = new HashSet<>();

        parsers.forEach(parser -> {
            extensions.addAll(parser.getSupportedFileExtensions());
        });
        return extensions;
    }
}
