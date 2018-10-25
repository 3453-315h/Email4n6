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
package com.github.email4n6.view.tabs.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;

import com.github.email4n6.utils.PathUtils;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;

/**
 * This class searches through the index.
 *
 * @author Marten4n6
 */
@Slf4j
public class SearchModel {

    private IndexSearcher searcher;
    private QueryParser parser;

    public SearchModel(String caseName) {
        try {
            searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(
                       new File(PathUtils.getIndexPath(caseName)).toPath())));
            parser = new QueryParser("searchable_text", new StandardAnalyzer());
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * @param query   The string to search for.
     * @param maxHits The maximum amount of documents returned.
     * @return A list of documents of the search query.
     */
    public List<Document> search(String query, int maxHits) {
        try {
            List<Document> documents = new ArrayList<>();

            for (ScoreDoc hit : searcher.search(parser.parse(query), maxHits).scoreDocs) {
                documents.add(searcher.doc(hit.doc));
            }
            return documents;
        } catch (ParseException | NullPointerException | IOException ex) {
            // TODO - Throw these exceptions and make the caller catch them.
            log.error(ex.getMessage());

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "", ButtonType.CLOSE);
                Label message = new Label(ex.getMessage());
                message.setWrapText(true);

                alert.getDialogPane().setContent(message);
                alert.showAndWait();
            });
            return new ArrayList<>(0);
        } catch (IllegalStateException ex) {
            log.error(ex.getMessage(), ex);
            return new ArrayList<>(0);
        }
    }

    /**
     * @param query   The query to search for.
     * @param maxHits The maximum amount of documents returned.
     * @return A list of documents of the search query.
     */
    public List<Document> search(Query query, int maxHits) {
        try {
            List<Document> documents = new ArrayList<>();

            for (ScoreDoc hit : searcher.search(query, maxHits).scoreDocs) {
                documents.add(searcher.doc(hit.doc));
            }
            return documents;
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
            return new ArrayList<>(0);
        }
    }

    /**
     * @return A set of all indexed fields.
     */
    public Set<String> getIndexedFields() {
        Set<String> fields = new HashSet<>();

        for (LeafReaderContext leafReaderContext : searcher.getIndexReader().leaves()) {
            for (FieldInfo fieldInfo : leafReaderContext.reader().getFieldInfos()) {
                fields.add(fieldInfo.name);
            }
        }
        return fields;
    }
}
