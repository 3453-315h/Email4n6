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

package com.github.email4n6.parser.impl.pst;

import com.github.email4n6.model.Settings;
import com.github.email4n6.parser.spi.ParserConfiguration;
import com.pff.*;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.h2.jdbc.JdbcSQLException;

import javax.swing.text.BadLocationException;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Indexes PST messages.
 *
 * @author Marten4n6
 */
@Slf4j
class PSTIndexer {

    private ParserConfiguration configuration;
    private SimpleDateFormat DATE_FORMAT;

    PSTIndexer(ParserConfiguration configuration) {
        this.configuration = configuration;
        this.DATE_FORMAT = new SimpleDateFormat(Settings.get(configuration.getCurrentCase().getName(), "DateFormat"));
    }

    /**
     * Indexes the PSTMessage.
     */
    void index(PSTMessage message, PSTFile pstFile, PSTFolder folder) {
        Document document = new Document();

        // Most important part of indexing!
        // See the method's JavaDoc for more information.
        addStringField(document, "ID", IDGenerator.getID(message, pstFile), true);

        // Used to get all messages from a specific PSTFolder
        // just by searching for the folder ID.
        addStringField(document, "FolderID", IDGenerator.getID(folder), true);

        // Index all the things...
        addTextField(document, "Subject", message.getSubject(), false);
        if (message.getSenderAddrtype().equals("EX")) {
            addTextField(document, "From", message.getEmailAddress(), true);
        } else if (message.getSenderEmailAddress().contains("@")) {
            addTextField(document, "From", message.getSenderEmailAddress(), true);
        } else {
            // The sender's email contains /O=EXCHANGELABS/OU=EXCHANGE ADMINISTRATIVE GROUP
            // so prefer the sender's name instead.
            addTextField(document, "From", message.getSenderName(), true);
        }

        try {
            for (int i = 0; i < message.getNumberOfRecipients(); i++) {
                PSTRecipient recipient = message.getRecipient(i);

                addTextField(document, "To", recipient.getSmtpAddress(), false);
            }
        } catch (PSTException | IOException ex) {
            log.error(ex.getMessage(), ex);
        }

        addTextField(document, "CC", message.getDisplayCC(), false);
        addTextField(document, "Headers", message.getTransportMessageHeaders(), false);
        addDateTextField(document, "ReceivedTime", message.getMessageDeliveryTime(), false);
        addDateTextField(document, "SubmitTime", message.getClientSubmitTime(), false);

        try {
            // A body can be either RTF, HTML or plaintext.
            String body = "";

            if (!message.getRTFBody().isEmpty()) {
                try {
                    RTFEditorKit rtfParser = new RTFEditorKit();
                    javax.swing.text.Document rtfDocument = rtfParser.createDefaultDocument();
                    rtfParser.read(new ByteArrayInputStream(message.getRTFBody().getBytes()), rtfDocument, 0);

                    body = rtfDocument.getText(0, rtfDocument.getLength());
                } catch (BadLocationException ex) {
                    log.error("Failed to parse RTF: {}", ex.getMessage());
                }
            } else if (!message.getBodyHTML().isEmpty()) {
                body = message.getBodyHTML();
            } else if (!message.getBody().isEmpty()) {
                body = message.getBody();
            }

            addTextField(document, "Body", body, false);
        } catch (PSTException | IOException ex) {
            log.error("Failed to get RTF body: {}", ex.getMessage(), ex);

            // Not sure if it's possible for there to be a
            // HTML/plaintext body as a fallback.
            log.debug("-------------------------------------");
            log.debug("Possibly fallback?");
            log.debug("HTML body: {}", message.getBodyHTML());
            log.debug("Plaintext body: {}", message.getBody());
            log.debug("-------------------------------------");
        }

        if (message.hasAttachments()) {
            for (int i = 0; i < message.getNumberOfAttachments(); i++) {
                try {
                    PSTAttachment attachment = message.getAttachment(i);

                    addTextField(document, "AttachmentName", attachment.getLongFilename(), false);
                } catch (PSTException | IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        }

        try {
            configuration.getIndexer().getIndexWriter().addDocument(document);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * Indexes the PSTAppointment.
     */
    void index(PSTAppointment appointment, PSTFile pstFile, PSTFolder folder) {
        Document document = new Document();

        // Most important part of indexing!
        // See the method's JavaDoc for more information.
        addStringField(document, "ID", IDGenerator.getID(appointment, pstFile), true);

        // Used to get all messages from a specific PSTFolder
        // just by searching for the folder ID.
        addStringField(document, "FolderID", IDGenerator.getID(folder), true);

        // Index all the things...
        addTextField(document, "Subject", appointment.getSubject(), false);
        addTextField(document, "AllAttendees", appointment.getAllAttendees(), false);
        addDateTextField(document, "StartTime", appointment.getStartTime(), false);
        addDateTextField(document, "EndTime", appointment.getEndTime(), false);

        try {
            configuration.getIndexer().getIndexWriter().addDocument(document);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * Indexes the PSTContact.
     */
    void index(PSTContact contact, PSTFile pstFile, PSTFolder folder) {
        Document document = new Document();

        // Most important part of indexing!
        // See the method's JavaDoc for more information.
        addStringField(document, "ID", IDGenerator.getID(contact, pstFile), true);

        // Used to get all messages from a specific PSTFolder
        // just by searching for the folder ID.
        addStringField(document, "FolderID", IDGenerator.getID(folder), true);

        // Index all the things...
        addTextField(document, "Subject", contact.getSubject(), false);

        log.debug("Subject: {}", contact.getSubject());

        try {
            configuration.getIndexer().getIndexWriter().addDocument(document);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * Helper method.
     */
    private void addStringField(Document document, String key, String value, boolean stored) {
        if (!value.isEmpty()) {
            if (stored) {
                document.add(new StringField(key, value, Field.Store.YES));
            } else {
                document.add(new StringField(key, value, Field.Store.NO));
            }
        }
    }

    /**
     * Helper method.
     */
    private void addTextField(Document document, String key, String value, boolean stored) {
        if (!value.isEmpty()) {
            if (stored) {
                document.add(new TextField(key, value, Field.Store.YES));
            } else {
                document.add(new TextField(key, value, Field.Store.NO));
            }
        }
    }

    /**
     * Helper method.
     */
    private void addDateTextField(Document document, String key, Date value, boolean stored) {
        if (value != null) {
            if (stored) {
                document.add(new TextField(key, DATE_FORMAT.format(value), Field.Store.YES));
            } else {
                document.add(new TextField(key, DATE_FORMAT.format(value), Field.Store.NO));
            }
        }
    }
}
