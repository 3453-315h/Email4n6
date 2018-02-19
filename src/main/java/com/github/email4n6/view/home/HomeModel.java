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

package com.github.email4n6.view.home;

import com.github.email4n6.model.casedao.Case;
import com.github.email4n6.model.casedao.CaseDAO;
import com.github.email4n6.model.casedao.JSONCaseDAO;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The model for the home tabs.
 *
 * @author Marten4n6
 */
@Slf4j
public class HomeModel {

    /**
     * Updates the case's human readable size.
     */
    public void updateCaseSize(Case caseObject, String size) {
        CaseDAO caseDAO = new JSONCaseDAO();

        caseObject.setSize(size);
        caseDAO.updateCase(caseObject);
    }

    /**
     * @param sources                 A set containing either a single folder or a list of file paths.
     * @param supportedFileExtensions A set of all supported file extensions.
     * @param extractSubFolders       True if sub-folders should be walked.
     * @return The size of the source in a human readable format.
     */
    public String getSourceSize(Set<String> sources, Set<String> supportedFileExtensions, boolean extractSubFolders) {
        final AtomicLong sourceSize = new AtomicLong(0);

        if (new File(sources.iterator().next()).isDirectory()) {
            try {
                Files.walkFileTree(Paths.get(sources.iterator().next()), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                        if (containsExtension(path.toFile(), supportedFileExtensions)) {
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
     * @return True if the file's extension is in the list of extensions.
     */
    private boolean containsExtension(File file, Set<String> extensions) {
        boolean containsExtension = false;

        for (String extension : extensions) {
            if (file.getName().toLowerCase().endsWith(extension.toLowerCase())) {
                containsExtension = true;
            }
        }
        return containsExtension;
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
