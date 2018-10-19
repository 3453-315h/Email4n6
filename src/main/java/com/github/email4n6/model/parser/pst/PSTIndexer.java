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
package com.github.email4n6.model.parser.pst;

import com.github.email4n6.model.Settings;
import com.github.email4n6.model.parser.ParserConfiguration;
import com.pff.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

import javax.swing.text.BadLocationException;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * Indexes PST messages.
 * If a message is not given an "id" and "folder_id" it will not be retrievable.
 *
 * @author Marten4n6
 */
@Slf4j
class PSTIndexer {

    private ParserConfiguration configuration;
    private SimpleDateFormat DATE_FORMAT;

    PSTIndexer(ParserConfiguration configuration) {
        this.configuration = configuration;
        this.DATE_FORMAT = new SimpleDateFormat(Settings.get(configuration.getCurrentCase().getName(), "date_format"));
    }

    /**
     * Indexes the PSTObject, automatically detects it's message type.
     */
    void index(PSTObject pstObject, String pstFileID, String folderID) {
        // Support types:
        // - PSTActivity represents Journal entries
        // - PSTAppointment is for Calendar items
        // - PSTContact is for contacts
        // - PSTRss represents an RSS item
        // - PSTTask represents Task items
        // - PSTMessage is a regular message
        if (pstObject instanceof PSTActivity) {
            indexActivity((PSTActivity)pstObject, pstFileID, folderID);
        } else if (pstObject instanceof PSTAppointment) {
            indexAppointment((PSTAppointment)pstObject, pstFileID, folderID);
        } else if (pstObject instanceof PSTContact) {
            indexContact((PSTContact)pstObject, pstFileID, folderID);
        } else if (pstObject instanceof PSTRss) {
            indexRSS((PSTRss)pstObject, pstFileID, folderID);
        } else if (pstObject instanceof PSTTask) {
            indexTask((PSTTask)pstObject, pstFileID, folderID);
        } else {
            // Assume this is a regular message.
            indexMessage((PSTMessage)pstObject, pstFileID, folderID);
        }
    }

    private void indexActivity(PSTActivity activity, String pstFileID, String folderID) {
        log.info("This message is an activity!");
    }

    private void indexAppointment(PSTAppointment appointment, String pstFileID, String folderID) {
        Document document = new Document();
        StringBuilder searchableText = new StringBuilder();

        // Fields which uniquely identify this item.
        addStringField(document, "id", IDGenerator.getID(appointment, pstFileID));
        addStringField(document, "folder_id", folderID);

        // Common
        addTextField(document, "subject", appointment.getSubject(), searchableText);
        addTextField(document, "headers", appointment.getTransportMessageHeaders(), searchableText);
        addTextField(document, "body", getBody(appointment), searchableText);

        // Appointment related
        addTextField(document, "all_attendees", appointment.getAllAttendees(), searchableText);
        addTextField(document, "location", appointment.getLocation(), searchableText);
        addDateTextField(document, "start_time", appointment.getStartTime(), searchableText);
        addDateTextField(document, "end_time", appointment.getEndTime(), searchableText);

        // Online meeting properties
        if (appointment.isOnlineMeeting()) {
            addTextField(document, "net_meeting_server", appointment.getNetMeetingServer(), searchableText);
            addTextField(document, "net_organizer_alias", appointment.getNetMeetingOrganizerAlias(), searchableText);
            addTextField(document, "net_document_pathname", appointment.getNetMeetingDocumentPathName(), searchableText);
            addTextField(document, "net_show_url", appointment.getNetShowURL(), searchableText);
        }

        addSearchableField(document, searchableText);

        try {
            configuration.getIndexer().getIndexWriter().addDocument(document);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

    }

    private void indexContact(PSTContact contact, String pstFileID, String folderID) {
        Document document = new Document();
        StringBuilder searchableText = new StringBuilder();

        // Fields which uniquely identify this item.
        addStringField(document, "id", IDGenerator.getID(contact, pstFileID));
        addStringField(document, "folder_id", folderID);

        // Common
        addTextField(document, "subject", contact.getSubject(), searchableText);

        // Contact related
        addTextField(document, "given_name", contact.getGivenName(), searchableText);
        addTextField(document, "surname", contact.getSurname(), searchableText);
        addTextField(document, "smtp_address", contact.getSMTPAddress(), searchableText);
        addTextField(document, "mobile_phone_number", contact.getMobileTelephoneNumber(), searchableText);
        addTextField(document, "other_phone_number", contact.getOtherTelephoneNumber(), searchableText);

        addSearchableField(document, searchableText);

        try {
            configuration.getIndexer().getIndexWriter().addDocument(document);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void indexRSS(PSTRss rss, String pstFileID, String folderID) {
        Document document = new Document();
        StringBuilder searchableText = new StringBuilder();

        // Fields which uniquely identify this item.
        addStringField(document, "id", IDGenerator.getID(rss, pstFileID));
        addStringField(document, "folder_id", folderID);

        // Common
        addTextField(document, "subject", rss.getSubject(), searchableText);
        addTextField(document, "headers", rss.getTransportMessageHeaders(), searchableText);
        addTextField(document, "body", getBody(rss), searchableText);

        addSearchableField(document, searchableText);

        try {
            configuration.getIndexer().getIndexWriter().addDocument(document);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void indexTask(PSTTask task, String pstFileID, String folderID) {
        Document document = new Document();
        StringBuilder searchableText = new StringBuilder();

        // Fields which uniquely identify this item.
        addStringField(document, "id", IDGenerator.getID(task, pstFileID));
        addStringField(document, "folder_id", folderID);

        // Common
        addTextField(document, "subject", task.getSubject(), searchableText);
        addTextField(document, "body", getBody(task), searchableText);

        // Task related
        addTextField(document, "task_owner", task.getTaskOwner(), searchableText);
        addTextField(document, "task_assigner", task.getTaskAssigner(), searchableText);

        addSearchableField(document, searchableText);

        try {
            configuration.getIndexer().getIndexWriter().addDocument(document);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void indexMessage(PSTMessage message, String pstFileID, String folderID) {
        Document document = new Document();
        StringBuilder searchableText = new StringBuilder();

        // Fields which uniquely identify this item.
        addStringField(document, "id", IDGenerator.getID(message, pstFileID));
        addStringField(document, "folder_id", folderID);

        // Message related
        addTextField(document, "subject", message.getSubject(), searchableText);
        addTextField(document, "body", getBody(message), searchableText);

        try {
            for (int i = 0; i < message.getNumberOfRecipients(); i++) {
                PSTRecipient recipient = message.getRecipient(i);

                addTextField(document, "to", recipient.getSmtpAddress(), searchableText);
            }
        } catch (PSTException | IOException ex) {
            log.error(ex.getMessage(), ex);
        }

        addTextField(document, "cc", message.getDisplayCC(), searchableText);
        addTextField(document, "headers", message.getTransportMessageHeaders(), searchableText);
        addDateTextField(document, "received_time", message.getMessageDeliveryTime(), searchableText);
        addDateTextField(document, "submit_time", message.getClientSubmitTime(), searchableText);

        if (message.hasAttachments()) {
            for (int i = 0; i < message.getNumberOfAttachments(); i++) {
                try {
                    PSTAttachment attachment = message.getAttachment(i);

                    addTextField(document, "attachment_name", attachment.getLongFilename(), searchableText);
                } catch (PSTException | IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            }
        }

        addSearchableField(document, searchableText);

        try {
            configuration.getIndexer().getIndexWriter().addDocument(document);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private String getBody(PSTMessage message) {
        try {
            // A body can be either RTF, HTML or plaintext.
            String body = "";

            if (!message.getRTFBody().isEmpty()) {
                try {
                    RTFEditorKit rtfParser = new RTFEditorKit();
                    javax.swing.text.Document rtfDocument = rtfParser.createDefaultDocument();
                    rtfParser.read(new ByteArrayInputStream(message.getRTFBody().getBytes()), rtfDocument, 0);

                    body = rtfDocument.getText(0, rtfDocument.getLength());
                } catch (BadLocationException | NumberFormatException ex) {
                    log.error("Failed to parse RTF: {}", ex.getMessage());
                }
            } else if (!message.getBodyHTML().isEmpty()) {
                body = message.getBodyHTML();
            } else if (!message.getBody().isEmpty()) {
                body = message.getBody();
            }

            return body;
        } catch (PSTException | IOException ex) {
            log.error("Failed to get RTF body: {}", ex.getMessage(), ex);
            return "";
        }
    }

    private void addStringField(Document document, String key, String value) {
        if (!value.isEmpty()) {
            document.add(new StringField(key, value, Field.Store.YES));
        }
    }

    private void addTextField(Document document, String key, String value, StringBuilder searchableText) {
        if (!value.isEmpty()) {
            document.add(new TextField(key, value, Field.Store.NO));
            searchableText.append(value).append(" ");
        }
    }

    private void addDateTextField(Document document, String key, Date value, StringBuilder searchableText) {
        if (value != null) {
            document.add(new TextField(key, DATE_FORMAT.format(value), Field.Store.NO));
            searchableText.append(value).append(" ");
        }
    }

    private void addSearchableField(Document document, StringBuilder searchableText) {
        document.add(new TextField("searchable_text", searchableText.toString(), Field.Store.NO));
    }
}
