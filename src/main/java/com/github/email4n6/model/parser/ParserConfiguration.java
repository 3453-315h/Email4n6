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

import com.github.email4n6.model.Case;
import com.github.email4n6.model.Indexer;
import com.github.email4n6.view.tabs.home.loading.LoadingStage;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import lombok.Builder;
import lombok.Getter;

/**
 * Builder containing configuration options used when parsing.
 *
 * @author Marten4n6
 */
@Builder
public class ParserConfiguration {

    /**
     * The current case to interact with.
     */
    private @Getter Case currentCase;

    /**
     * The indexer used for indexing messages.
     */
    private @Getter Indexer indexer;

    /**
     * The loading stage parsers can show progress with.
     */
    private @Getter LoadingStage loadingStage;

    /**
     * This listener should be called EVERY TIME the parseFile method finishes.
     * Needed so the FileParser knows when all parsers are finished with current file.
     */
    private @Getter EventHandler<ActionEvent> finishedListener;
}
