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
package com.github.email4n6.view.tabs.home;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import com.github.email4n6.model.Case;
import com.github.email4n6.model.Settings;
import com.github.email4n6.model.parser.FileParser;
import com.github.email4n6.model.parser.ParserFactory;
import com.github.email4n6.utils.PathUtils;
import com.github.email4n6.view.tabs.home.loading.LoadingStage;
import com.google.gson.Gson;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.stage.Window;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Model for the home tab, handles retrieving and persisting cases.
 * This model also may hold any shared objects for the current case.
 *
 * @author Marten4n6
 */
@Slf4j
public class HomeModel {

    /**
     * The currently open case, otherwise null.
     */
    private @Getter Case currentCase;

    /**
     * Source which handles communicating with the database.
     *
     * @see <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a>
     */
    private @Getter HikariDataSource database;

    /**
     * Listeners
     */
    private @Setter FileParser.FinishedListener onFinishedParsing;
    private @Setter EventHandler<ActionEvent> onActiveCaseClosed;

    /**
     * Opens the case and initializes shared objects related to it.
     * The case will be created if it doesn't yet exist.
     */
    void openCase(Case caseObject, Window ownerWindow) {
        log.info("Opening case \"{}\"...", caseObject.getName());

        if (currentCase != null) {
            // Clean up objects of the previously open case.
            closeCase();
        }

        currentCase = caseObject;

        // Initialize the database.
        log.info("Initializing the database...");

        HikariConfig config = new HikariConfig();
        config.setAutoCommit(false);
        config.setJdbcUrl("jdbc:h2:" + PathUtils.getCasePath(caseObject.getName()) + File.separator + "Email4n6");

        database = new HikariDataSource(config);

        if (!isExistingCase(caseObject.getName())) {
            // This case doesn't yet exist...
            log.info("This case doesn't yet exist, creating...");

            new File(PathUtils.getCasePath(caseObject.getName())).mkdir();
            new File(PathUtils.getIndexPath(caseObject.getName())).mkdir();

            // Set default settings.
            Settings.set(caseObject.getName(), "date_format", "EEE, d MMM yyyy HH:mm:ss");
            Settings.set(caseObject.getName(), "search_limit", "100");

            try {
                @Cleanup Connection connection = database.getConnection();
                @Cleanup Statement statement = connection.createStatement();

                statement.execute("CREATE TABLE IF NOT EXISTS Bookmarks(id VARCHAR(100) PRIMARY KEY)");
                statement.execute("CREATE TABLE IF NOT EXISTS Tags(id VARCHAR(100) PRIMARY KEY, tag VARCHAR(100))");
                connection.commit();
            } catch (SQLException ex) {
                log.error(ex.getMessage(), ex);
            }
        }

        // Process the sources of this case using the FileParser.
        LoadingStage loadingStage = new LoadingStage(ownerWindow);

        ParserFactory parserFactory = new ParserFactory();
        FileParser fileParser = new FileParser(parserFactory, caseObject, loadingStage);

        loadingStage.show();

        new Thread(new Task<Void>() {

            @Override
            protected Void call() {
                if (caseObject.getSize().equals("Not calculated yet.")) {
                    // Update the source size.
                    loadingStage.setStatus("Calculating the size of the source(s)...");
                    log.debug("Calculating the size of the source(s)...");

                    String sourceSize = getSourceSize(caseObject.getSources(), caseObject.isSubFolders());
                    persistCase(caseObject);

                    caseObject.setSize(sourceSize);

                    loadingStage.setStatus("Waiting for parsers to finish...");
                    log.debug("Source(s) size: {}", sourceSize);
                }

                File firstSource = new File(caseObject.getSources().iterator().next());

                fileParser.setOnParsingFinished(onFinishedParsing);

                if (firstSource.isDirectory()) {
                    fileParser.parseFolder(firstSource.getPath(), caseObject.isSubFolders());
                } else {
                    fileParser.parseFiles(caseObject.getSources());
                }

                return null;
            }
        }).start();
    }

    /**
     * Closes the currently open case.
     */
    private void closeCase() {
        onActiveCaseClosed.handle(null);
        database.close();
    }

    /**
     * Writes the case object to disk.
     */
    private void persistCase(Case caseObject) {
        // It wouldn't be a smart idea to store this object
        // in the database, because if you'd want to retrieve EVERY
        // case object you'd need to connect to each database instead of
        // just reading from a file.
        try {
            Files.write(
                    Paths.get(PathUtils.getCasePath(caseObject.getName()) + File.separator + "case.json"),
                    new Gson().toJson(caseObject).getBytes(), StandardOpenOption.CREATE_NEW
            );
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * Removes the case from disk.
     */
    void removeCase(String caseName) {
        try {
            Files.walk(Paths.get(PathUtils.getCasePath(caseName)))
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * @return True if the case exists.
     */
    boolean isExistingCase(String caseName) {
        return new File(PathUtils.getCasePath(caseName) + File.separator + "case.json").exists();
    }

    /**
     * @return A list of case names.
     */
    List<String> getCases() {
        List<String> caseNames = new ArrayList<>();

        try {
            Files.list(Paths.get(PathUtils.getCasesPath())).forEach(path -> {
                caseNames.add(path.toFile().getName());
            });
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        return caseNames;
    }

    /**
     * @return The case object of the given case name.
     */
    Case getCaseObject(String caseName) {
        try {
            String jsonObject = Files.newBufferedReader(
                    Paths.get(PathUtils.getCasePath(caseName) + File.separator + "case.json")
            ).readLine();

            return new Gson().fromJson(jsonObject, Case.class);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * @param sources                 A set containing either a single folder or a list of file paths.
     * @param extractSubFolders       True if sub-folders should be walked.
     * @return The size of the source in a human readable format.
     */
    private String getSourceSize(Set<String> sources, boolean extractSubFolders) {
        final AtomicLong sourceSize = new AtomicLong(0);

        if (new File(sources.iterator().next()).isDirectory()) {
            try {
                Files.walkFileTree(Paths.get(sources.iterator().next()), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        String fileName = path.toFile().getName().toLowerCase();

                        if (fileName.endsWith(".pst") && fileName.endsWith(".ost")) {
                            sourceSize.addAndGet(path.toFile().length());
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                        if (!extractSubFolders && !path.toString().equals(sources.iterator().next())) {
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
        } else {
            for (String source : sources) {
                sourceSize.addAndGet(new File(source).length());
            }
        }
        return humanReadableByteCount(sourceSize.get());
    }

    /**
     * @return A human readable byte size.
     */
    private String humanReadableByteCount(long bytes) {
        boolean si = true;
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
