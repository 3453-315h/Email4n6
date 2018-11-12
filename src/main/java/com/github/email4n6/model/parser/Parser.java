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
package com.github.email4n6.model.parser;

import java.io.File;
import java.util.Set;

import com.github.email4n6.view.tabs.tree.TreeObject;

import javafx.scene.control.TreeItem;

/**
 * Interface for parsing files.
 *
 * @author Marten4n6
 */
public interface Parser {

    /**
     * @return The name of this parser.
     */
    String getName();

    /**
     * @return A set of file extensions this parser accepts.
     */
    Set<String> getSupportedFileExtensions();

    /**
     * Parses the file.
     *
     * @param file          The file to parse.
     * @param configuration The configuration options of this parsers.
     * @param totalFiles    The amount of files this parser should expect.
     */
    TreeItem<TreeObject> parseFile(File file, ParserConfiguration configuration, int totalFiles);
}
