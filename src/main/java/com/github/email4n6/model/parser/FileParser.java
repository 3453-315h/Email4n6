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
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.email4n6.model.Case;
import com.github.email4n6.model.Indexer;
import com.github.email4n6.view.tabs.home.loading.LoadingStage;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * The class handles parsing the case's source files.
 *
 * @author Marten4n6
 */
@Slf4j
public class FileParser {

    private ParserFactory parserFactory;
    private Case currentCase;
    private LoadingStage loadingStage;
    private Indexer indexer;

    private @Setter FinishedListener onParsingFinished;

    public FileParser(ParserFactory parserFactory, Case currentCase, LoadingStage loadingStage) {
        this.parserFactory = parserFactory;
        this.currentCase = currentCase;
        this.loadingStage = loadingStage;
        this.indexer = new Indexer(currentCase.getName());
    }

    /**
     * Calls the appropriate parsers on the files.
     *
     * @param sources The paths of the files to parse.
     */
    public void parseFiles(Set<String> sources) {
        sources.forEach(source -> {
            File file = new File(source);

            CountDownLatch countDownLatch = new CountDownLatch(1);
            AtomicInteger runningParsersForFile = new AtomicInteger(0);

            // Call all supported parsers on the file.
            parserFactory.getParsers(getFileExtension(file)).forEach(parser -> {
                runningParsersForFile.incrementAndGet();

                EventHandler<ActionEvent> finishedListener = (event) -> {
                    runningParsersForFile.decrementAndGet();

                    if (runningParsersForFile.get() == 0) {
                        countDownLatch.countDown(); // Continue to the next file.
                    }
                };

                ParserConfiguration configuration = ParserConfiguration.builder()
                        .currentCase(currentCase)
                        .indexer(indexer)
                        .loadingStage(loadingStage)
                        .finishedListener(finishedListener).build();

                int expectedFiles = 0;

                for (String filePath : sources) {
                    if (parser.getSupportedFileExtensions().contains(getFileExtension(new File(filePath)))) {
                        expectedFiles++;
                    }
                }

                log.debug("The \"{}\" parser is expecting {} file(s).", parser.getName(), expectedFiles);

                parser.parseFile(file, configuration, expectedFiles);
            });

            try {
                log.debug("Waiting for parsers to finish with \"{}\"...", file.getName());
                countDownLatch.await(); // Wait for parsers to finish before continuing.
            } catch (InterruptedException ex) {
                log.error(ex.getMessage(), ex);
            }
        });

        onParsingFinished.finished(currentCase, indexer, loadingStage);
    }

    /**
     * Walks the folder and calls the appropriate parsers on the files.
     *
     * @param folderPath The path to the folder to parse.
     */
    public void parseFolder(String folderPath, boolean extractSubFolders) {
        Set<String> sources = new HashSet<>();
        Set<String> supportedFileExtensions = parserFactory.getAllSupportedFileExtensions();

        try {
            Files.walkFileTree(Paths.get(folderPath), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                    if (supportedFileExtensions.contains(getFileExtension(path.toFile()))) {
                        sources.add(path.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    if (!extractSubFolders && !path.toString().equals(folderPath)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    } else {
                        return FileVisitResult.CONTINUE;
                    }
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException ex) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

        parseFiles(sources);
    }

    /**
     * @return The file's extension.
     */
    private String getFileExtension(File file) {
        String extension = "";
        int i = file.getName().lastIndexOf('.');

        if (i >= 0) {
            extension = file.getName().substring(i + 1);
        }
        return extension;
    }

    /**
     * Listener which gets called when parsing finishes.
     */
    public interface FinishedListener {

        void finished(Case currentCase, Indexer indexer, LoadingStage loadingStage);
    }
}
