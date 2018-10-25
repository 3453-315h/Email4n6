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

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.github.email4n6.utils.PathUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * This class handles indexing.
 *
 * @author Marten4n6
 */
@Slf4j
public class Indexer {

    private @Getter IndexWriter indexWriter;

    public Indexer(String caseName) {
        try {
            // https://lucene.apache.org/core/7_4_0/core/org/apache/lucene/store/FSDirectory.html
            // https://lucene.apache.org/core/7_4_0/core/org/apache/lucene/index/IndexWriterConfig.html
            // https://lucene.apache.org/core/7_4_0/core/org/apache/lucene/index/IndexWriter.html
            log.info("Initializing the indexer...");

            Directory directory = FSDirectory.open(Paths.get(PathUtils.getIndexPath(caseName)));
            IndexWriterConfig configuration = new IndexWriterConfig();
            indexWriter = new IndexWriter(directory, configuration);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * Commits and closes the indexer.
     */
    public void close() {
        try {
            log.info("Closing the indexer...");

            indexWriter.commit();
            indexWriter.close();
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
