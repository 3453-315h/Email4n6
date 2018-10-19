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

import com.github.email4n6.model.message.AttachmentRow;
import com.github.email4n6.model.message.MessageRow;
import com.github.email4n6.model.message.MessageValue;
import com.github.email4n6.view.tabs.search.SearchModel;
import com.github.email4n6.model.Settings;
import com.github.email4n6.model.Case;
import com.github.email4n6.model.message.factory.MessageFactory;
import com.github.email4n6.view.tabs.bookmarks.BookmarksModel;
import com.github.email4n6.view.tabs.bookmarks.TagModel;
import com.github.email4n6.view.tabs.tree.TreeObject;
import com.pff.*;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import javax.swing.text.BadLocationException;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Factory for creating messages related to this parser.
 *
 * @author Marten4n6
 */
@Slf4j
public class PSTMessageFactory implements MessageFactory {

    /**
     * Stores a list of <b>PSTFile</b> objects we are currently working with,
     * needed so we can retrieve any <b>PSTObject</b> with detectAndLoadPSTObject.
     *
     * <p>
     *     Static since we don't want a reference to this class in the PSTParser class. <br/>
     *     Cleared when a new PSTParser instance is created.
     * </p>
     *
     * @see IDGenerator
     */
    // Best solution I can think of...
    private static final HashMap<String, PSTFile> fileFromID = new HashMap<>();

    private Case currentCase;
    private BookmarksModel bookmarksModel;
    private TagModel tagModel;
    private SearchModel searchModel;

    static void addPSTFile(PSTFile pstFile) {
        log.debug("Adding new PST file...");
        fileFromID.put(IDGenerator.getID(pstFile), pstFile);
    }

    static void clearPSTFiles() {
        log.debug("Cleaning up any previous PST files...");
        fileFromID.clear();
    }

    public PSTMessageFactory(Case currentCase, BookmarksModel bookmarksModel, TagModel tagModel, SearchModel searchModel) {
        this.currentCase = currentCase;
        this.bookmarksModel = bookmarksModel;
        this.tagModel = tagModel;
        this.searchModel = searchModel;
    }

    @Override
    public MessageRow getMessageRow(String id) {
        try {
            // If the ID belongs to this parser it will have the
            // descriptor index as the first part and the PSTFile ID
            // as the second part of the ID.
            String pstFileID = id.split("-")[1];
            PSTFile pstFile = fileFromID.get(pstFileID);
            PSTObject pstObject = PSTObject.detectAndLoadPSTObject(pstFile, Long.parseLong(id.split("-")[0]));

            MessageRow.MessageRowBuilder messageRowBuilder = MessageRow.builder();
            String messageID = null;

            if (pstObject instanceof PSTContact) {
                // Contact
                PSTContact contact = (PSTContact) pstObject;
                messageID = IDGenerator.getID(contact, pstFileID);

                messageRowBuilder.subject(contact.getSubject());
                messageRowBuilder.size(contact.getMessageSize());
            } else if (pstObject instanceof PSTAppointment) {
                // Appointment
                PSTAppointment appointment = (PSTAppointment) pstObject;
                messageID = IDGenerator.getID(appointment, pstFileID);

                messageRowBuilder.subject(appointment.getSubject());
                messageRowBuilder.receivedDate(appointment.getMessageDeliveryTime());
                messageRowBuilder.size(appointment.getMessageSize());
            } else if (pstObject instanceof PSTTask) {
                log.debug("It's a PSTTask!");
            } else if (pstObject instanceof PSTRss) {
                log.debug("It's a PSTRss!");
            } else if (pstObject instanceof PSTActivity) {
                log.debug("It's a PSTActivity!");
            } else if (pstObject instanceof PSTMessage) {
                // Message
                PSTMessage message = (PSTMessage) pstObject;
                messageID = IDGenerator.getID(message, pstFileID);
                StringBuilder to = new StringBuilder();

                // Build a list of recipient emails.
                for (int i = 0; i < message.getNumberOfRecipients(); i++) {
                    PSTRecipient recipient = message.getRecipient(i);

                    to.append(recipient.getSmtpAddress());

                    if (i != message.getNumberOfRecipients() -1) {
                        to.append(", ");
                    }
                }

                messageRowBuilder.subject(message.getSubject());
                messageRowBuilder.to(to.toString());
                messageRowBuilder.receivedDate(message.getMessageDeliveryTime());
                messageRowBuilder.size(message.getMessageSize());
                messageRowBuilder.cc(message.getDisplayCC());

                if (message.getSenderEmailAddress().contains("@") && !message.getSenderAddrtype().equals("EX")) {
                    messageRowBuilder.from(message.getSenderEmailAddress());
                } else {
                    // The sender's email contains /O=EXCHANGELABS/OU=EXCHANGE ADMINISTRATIVE GROUP
                    // so prefer the sender's name instead.
                    messageRowBuilder.from(message.getSenderName());
                }
            }

            SimpleBooleanProperty bookmarkedProperty = new SimpleBooleanProperty(bookmarksModel.isBookmark(messageID));
            SimpleStringProperty tagProperty = new SimpleStringProperty(tagModel.getTag(messageID));

            messageRowBuilder.id(messageID);
            messageRowBuilder.bookmarked(bookmarkedProperty);
            messageRowBuilder.tag(tagProperty);

            MessageRow messageRow = messageRowBuilder.build();

            // Bookmark listener
            bookmarkedProperty.addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
                if (newValue) {
                    log.debug("Adding bookmark (callback) in thread {}: " + messageRow.getId(), Thread.currentThread().getId());
                    bookmarksModel.addBookmark(messageRow);
                } else {
                    log.debug("Removing bookmark (callback) in thread {}: " + messageRow.getId(), Thread.currentThread().getId());
                    bookmarksModel.removeBookmark(messageRow);
                }
            });
            // Tag listener
            tagProperty.addListener((ObservableValue<? extends String> observable, String oldValue, String newValue) -> {
                if (!newValue.equals(oldValue)) {
                    log.debug("Changing tag of \"{}\" to: {}", messageRow.getId(), newValue);
                    tagModel.setTag(messageRow.getId(), newValue);
                }
            });

            return messageRow;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            // Exceptions related to the ID.
            log.debug("This ID doesn't belong to the PSTParser.");
        } catch (IOException | PSTException ex) {
            // PST-related exceptions.
            log.error(ex.getMessage(), ex);
        } catch (Exception ex) {
            // Anything unexpected we haven't seen yet.
            log.error("Unknown error: {}", ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public MessageValue getMessageValue(String id) {
        try {
            // If the ID belongs to this parser it will have the
            // descriptor index as the first part and the PSTFile ID
            // as the second part of the ID.
            PSTFile pstFile = fileFromID.get(id.split("-")[1]);
            PSTObject pstObject = PSTObject.detectAndLoadPSTObject(pstFile, Long.parseLong(id.split("-")[0]));

            MessageValue.MessageValueBuilder messageValue = MessageValue.builder();
            final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(Settings.get(currentCase.getName(), "date_format"));

            if (pstObject instanceof PSTContact) {
                // Contact
                PSTContact contact = (PSTContact) pstObject;
                StringBuilder bodyBuilder = new StringBuilder();

                // Here we go...
                if (!contact.getAccount().isEmpty()) bodyBuilder.append("<b>Account: </b>").append(contact.getAccount()).append("<br/>");
                if (contact.getAnniversary() != null) bodyBuilder.append("<b>Anniversary: </b>").append(DATE_FORMAT.format(contact.getAnniversary())).append("<br/>");
                if (!contact.getAssistant().isEmpty()) bodyBuilder.append("<b>Assistant: </b>").append(contact.getAssistant()).append("<br/>");
                if (!contact.getAssistantTelephoneNumber().isEmpty()) bodyBuilder.append("<b>Assistant Telephone Number: </b>").append(contact.getAssistantTelephoneNumber()).append("<br/>");
                if (contact.getBirthday() != null) bodyBuilder.append("<b>Birthday: </b>").append(DATE_FORMAT.format(contact.getBirthday())).append("<br/>");
                if (!contact.getBusiness2TelephoneNumber().isEmpty()) bodyBuilder.append("<b>Business Telephone Number 2: </b>").append(contact.getBusiness2TelephoneNumber()).append("<br/>");
                if (!contact.getBusinessAddressCity().isEmpty()) bodyBuilder.append("<b>Business City: </b>").append(contact.getBusinessAddressCity()).append("<br/>");
                if (!contact.getBusinessAddressCountry().isEmpty()) bodyBuilder.append("<b>Business Country: </b>").append(contact.getBusinessAddressCountry()).append("<br/>");
                if (!contact.getBusinessAddressStateOrProvince().isEmpty()) bodyBuilder.append("<b>Business State/Province: </b>").append(contact.getBusinessAddressStateOrProvince()).append("<br/>");
                if (!contact.getBusinessAddressStreet().isEmpty()) bodyBuilder.append("<b>Business Street: </b>").append(contact.getBusinessAddressStreet()).append("<br/>");
                if (!contact.getBusinessFaxNumber().isEmpty()) bodyBuilder.append("<b>Business Fax Number: </b>").append(contact.getBusinessFaxNumber()).append("<br/>");
                if (!contact.getBusinessHomePage().isEmpty()) bodyBuilder.append("<b>Business Homepage: </b>").append(contact.getBusinessHomePage()).append("<br/>");
                if (!contact.getBusinessPoBox().isEmpty()) bodyBuilder.append("<b>Business P.O. Box: </b>").append(contact.getBusinessPoBox()).append("<br/>");
                if (!contact.getBusinessPostalCode().isEmpty()) bodyBuilder.append("<b>Business Postal Code: </b>").append(contact.getBusinessPostalCode()).append("<br/>");
                if (!contact.getBusinessTelephoneNumber().isEmpty()) bodyBuilder.append("<b>Business Telephone Number: </b>").append(contact.getBusinessTelephoneNumber()).append("<br/>");
                if (!contact.getCallbackTelephoneNumber().isEmpty()) bodyBuilder.append("<b>Callback Telephone Number: </b>").append(contact.getCallbackTelephoneNumber()).append("<br/>");
                if (!contact.getCarTelephoneNumber().isEmpty()) bodyBuilder.append("<b>Car Telephone Number: </b>").append(contact.getCarTelephoneNumber()).append("<br/>");
                if (!contact.getChildrensNames().isEmpty()) bodyBuilder.append("<b>Children's Names: </b>").append(contact.getChildrensNames()).append("<br/>");
                if (!contact.getCompanyMainPhoneNumber().isEmpty()) bodyBuilder.append("<b>Company Main Phone Number: </b>").append(contact.getCompanyMainPhoneNumber()).append("<br/>");
                if (!contact.getCompanyName().isEmpty()) bodyBuilder.append("<b>Company Name: </b>").append(contact.getCompanyName()).append("<br/>");
                if (!contact.getComputerNetworkName().isEmpty()) bodyBuilder.append("<b>Computer Network Name: </b>").append(contact.getComputerNetworkName()).append("<br/>");
                if (!contact.getCustomerId().isEmpty()) bodyBuilder.append("<b>Customer ID: </b>").append(contact.getCustomerId()).append("<br/>");
                if (!contact.getDepartmentName().isEmpty()) bodyBuilder.append("<b>Department Name: </b>").append(contact.getDepartmentName()).append("<br/>");
                if (!contact.getEmail1AddressType().isEmpty()) bodyBuilder.append("<b>Email Address Type: </b>").append(contact.getEmail1AddressType()).append("<br/>");
                if (!contact.getEmail1DisplayName().isEmpty()) bodyBuilder.append("<b>Email Display Name: </b>").append(contact.getEmail1DisplayName()).append("<br/>");
                if (!contact.getEmail1EmailAddress().isEmpty()) bodyBuilder.append("<b>Email Email Address: </b>").append(contact.getEmail1EmailAddress()).append("<br/>");
                if (!contact.getEmail1EmailType().isEmpty()) bodyBuilder.append("<b>Email Type: </b>").append(contact.getEmail1EmailType()).append("<br/>");
                if (!contact.getEmail1OriginalDisplayName().isEmpty()) bodyBuilder.append("<b>Email Original Display Name: </b>").append(contact.getEmail1OriginalDisplayName()).append("<br/>");
                if (!contact.getEmail2AddressType().isEmpty()) bodyBuilder.append("<b>Second Email Address Type: </b>").append(contact.getEmail2AddressType()).append("<br/>");
                if (!contact.getEmail2DisplayName().isEmpty()) bodyBuilder.append("<b>Second Email Display Name: </b>").append(contact.getEmail2DisplayName()).append("<br/>");
                if (!contact.getEmail2EmailAddress().isEmpty()) bodyBuilder.append("<b>Second Email Email Address: </b>").append(contact.getEmail2EmailAddress()).append("<br/>");
                if (!contact.getEmail2OriginalDisplayName().isEmpty()) bodyBuilder.append("<b>Second Email Original Display Name: </b>").append(contact.getEmail2OriginalDisplayName()).append("<br/>");
                if (!contact.getEmail3AddressType().isEmpty()) bodyBuilder.append("<b>Third Email Address Type: </b>").append(contact.getEmail3AddressType()).append("<br/>");
                if (!contact.getEmail3DisplayName().isEmpty()) bodyBuilder.append("<b>Third Email Display Name: </b>").append(contact.getEmail3DisplayName()).append("<br/>");
                if (!contact.getEmail3EmailAddress().isEmpty()) bodyBuilder.append("<b>Third Email Email Address: </b>").append(contact.getEmail3EmailAddress()).append("<br/>");
                if (!contact.getEmail3OriginalDisplayName().isEmpty()) bodyBuilder.append("<b>Third Email Original Display Name: </b>").append(contact.getEmail3OriginalDisplayName()).append("<br/>");
                if (!contact.getFax1EmailAddress().isEmpty()) bodyBuilder.append("<b>Fax Email Address: </b>").append(contact.getFax1EmailAddress()).append("<br/>");
                if (!contact.getFax1OriginalDisplayName().isEmpty()) bodyBuilder.append("<b>Fax Original Display Name: </b>").append(contact.getFax1OriginalDisplayName()).append("<br/>");
                if (!contact.getFax2EmailAddress().isEmpty()) bodyBuilder.append("<b>Second Fax Email Address: </b>").append(contact.getFax2EmailAddress()).append("<br/>");
                if (!contact.getFax2OriginalDisplayName().isEmpty()) bodyBuilder.append("<b>Second Fax Original Display Name: </b>").append(contact.getFax2OriginalDisplayName()).append("<br/>");
                if (!contact.getFax3EmailAddress().isEmpty()) bodyBuilder.append("<b>Third Fax Email Address: </b>").append(contact.getFax3EmailAddress()).append("<br/>");
                if (!contact.getFax3OriginalDisplayName().isEmpty()) bodyBuilder.append("<b>Third Fax Original Display Name: </b>").append(contact.getFax3OriginalDisplayName()).append("<br/>");
                if (!contact.getFileUnder().isEmpty()) bodyBuilder.append("<b>File Under: </b>").append(contact.getFileUnder()).append("<br/>");
                if (!contact.getFreeBusyLocation().isEmpty()) bodyBuilder.append("<b>Free Busy Location: </b>").append(contact.getFreeBusyLocation()).append("<br/>");
                if (!contact.getFtpSite().isEmpty()) bodyBuilder.append("<b>FTP Site: </b>").append(contact.getFtpSite()).append("<br/>");
                if (!contact.getGeneration().isEmpty()) bodyBuilder.append("<b>Generation: </b>").append(contact.getGeneration()).append("<br/>");
                if (!contact.getGivenName().isEmpty()) bodyBuilder.append("<b>Given Name: </b>").append(contact.getGivenName()).append("<br/>");
                if (!contact.getGovernmentIdNumber().isEmpty()) bodyBuilder.append("<b>Government ID Number: </b>").append(contact.getGovernmentIdNumber()).append("<br/>");
                if (!contact.getHobbies().isEmpty()) bodyBuilder.append("<b>Hobbies: </b>").append(contact.getHobbies()).append("<br/>");
                if (!contact.getHome2TelephoneNumber().isEmpty()) bodyBuilder.append("<b>Home 2 Telephone Number: </b>").append(contact.getHome2TelephoneNumber()).append("<br/>");
                if (!contact.getHomeAddress().isEmpty()) bodyBuilder.append("<b>Home Address: </b>").append(contact.getHomeAddress()).append("<br/>");
                if (!contact.getHomeAddressCity().isEmpty()) bodyBuilder.append("<b>Home City: </b>").append(contact.getHomeAddressCity()).append("<br/>");
                if (!contact.getHomeAddressCountry().isEmpty()) bodyBuilder.append("<b>Home Country: </b>").append(contact.getHomeAddressCountry()).append("<br/>");
                if (!contact.getHomeAddressPostOfficeBox().isEmpty()) bodyBuilder.append("<b>Home P.O. Box: </b>").append(contact.getHomeAddressPostOfficeBox()).append("<br/>");
                if (!contact.getHomeAddressPostalCode().isEmpty()) bodyBuilder.append("<b>Home Postal Code: </b>").append(contact.getHomeAddressPostalCode()).append("<br/>");
                if (!contact.getHomeAddressStateOrProvince().isEmpty()) bodyBuilder.append("<b>Home State/Province: </b>").append(contact.getHomeAddressStateOrProvince()).append("<br/>");
                if (!contact.getHomeAddressStreet().isEmpty()) bodyBuilder.append("<b>Home Street: </b>").append(contact.getHomeAddressStreet()).append("<br/>");
                if (!contact.getHomeFaxNumber().isEmpty()) bodyBuilder.append("<b>Home Fax Number: </b>").append(contact.getHomeFaxNumber()).append("<br/>");
                if (!contact.getHomeTelephoneNumber().isEmpty()) bodyBuilder.append("<b>Home Telephone Number: </b>").append(contact.getHomeTelephoneNumber()).append("<br/>");
                if (!contact.getInitials().isEmpty()) bodyBuilder.append("<b>Initials: </b>").append(contact.getInitials()).append("<br/>");
                if (!contact.getInstantMessagingAddress().isEmpty()) bodyBuilder.append("<b>Instant Messaging Address: </b>").append(contact.getInstantMessagingAddress()).append("<br/>");
                if (!contact.getIsdnNumber().isEmpty()) bodyBuilder.append("<b>ISDN Number: </b>").append(contact.getIsdnNumber()).append("<br/>");
                if (!contact.getKeyword().isEmpty()) bodyBuilder.append("<b>Keyword: </b>").append(contact.getKeyword()).append("<br/>");
                if (!contact.getLanguage().isEmpty()) bodyBuilder.append("<b>Language: </b>").append(contact.getLanguage()).append("<br/>");
                if (!contact.getLocation().isEmpty()) bodyBuilder.append("<b>Location: </b>").append(contact.getLocation()).append("<br/>");
                if (!contact.getManagerName().isEmpty()) bodyBuilder.append("<b>Manager Name: </b>").append(contact.getManagerName()).append("<br/>");
                if (!contact.getMhsCommonName().isEmpty()) bodyBuilder.append("<b>MHS Common Name: </b>").append(contact.getMhsCommonName()).append("<br/>");
                if (!contact.getMiddleName().isEmpty()) bodyBuilder.append("<b>Middle Name: </b>").append(contact.getMiddleName()).append("<br/>");
                if (!contact.getMobileTelephoneNumber().isEmpty()) bodyBuilder.append("<b>Mobile Telephone Number: </b>").append(contact.getMobileTelephoneNumber()).append("<br/>");
                if (!contact.getNickname().isEmpty()) bodyBuilder.append("<b>Nickname: </b>").append(contact.getNickname()).append("<br/>");
                if (!contact.getNote().trim().isEmpty()) bodyBuilder.append("<b>Note: </b>").append(contact.getNote()).append("<br/>");
                if (!contact.getOfficeLocation().isEmpty()) bodyBuilder.append("<b>Office Location: </b>").append(contact.getOfficeLocation()).append("<br/>");
                if (!contact.getOrganizationalIdNumber().isEmpty()) bodyBuilder.append("<b>Organizational ID Number: </b>").append(contact.getOrganizationalIdNumber()).append("<br/>");
                if (!contact.getOriginalDisplayName().isEmpty()) bodyBuilder.append("<b>Original Display Name: </b>").append(contact.getOriginalDisplayName()).append("<br/>");
                if (!contact.getOtherAddress().isEmpty()) bodyBuilder.append("<b>Other Address: </b>").append(contact.getOtherAddress()).append("<br/>");
                if (!contact.getOtherAddressCity().isEmpty()) bodyBuilder.append("<b>Other Address City: </b>").append(contact.getOtherAddressCity()).append("<br/>");
                if (!contact.getOtherAddressCountry().isEmpty()) bodyBuilder.append("<b>Other Address Country: </b>").append(contact.getOtherAddressCountry()).append("<br/>");
                if (!contact.getOtherAddressPostOfficeBox().isEmpty()) bodyBuilder.append("<b>Other Address P.O. Box: </b>").append(contact.getOtherAddressPostOfficeBox()).append("<br/>");
                if (!contact.getOtherAddressPostalCode().isEmpty()) bodyBuilder.append("<b>Other Address Postal Code: </b>").append(contact.getOtherAddressPostalCode()).append("<br/>");
                if (!contact.getOtherAddressStateOrProvince().isEmpty()) bodyBuilder.append("<b>Other Address State/Province: </b>").append(contact.getOtherAddressStateOrProvince()).append("<br/>");
                if (!contact.getOtherAddressStreet().isEmpty()) bodyBuilder.append("<b>Other Address Street: </b>").append(contact.getOtherAddressStreet()).append("<br/>");
                if (!contact.getOtherTelephoneNumber().isEmpty()) bodyBuilder.append("<b>Other Telephone Number: </b>").append(contact.getOtherTelephoneNumber()).append("<br/>");
                if (!contact.getPagerTelephoneNumber().isEmpty()) bodyBuilder.append("<b>Pager Telephone Number: </b>").append(contact.getPagerTelephoneNumber()).append("<br/>");
                if (!contact.getPersonalHomePage().isEmpty()) bodyBuilder.append("<b>Personal Homepage: </b>").append(contact.getPersonalHomePage()).append("<br/>");
                if (!contact.getPostalAddress().isEmpty()) bodyBuilder.append("<b>Postal Address: </b>").append(contact.getPostalAddress()).append("<br/>");
                if (contact.getPostalAddressId() != 0) bodyBuilder.append("<b>Account: </b>").append(contact.getPostalAddressId()).append("<br/>");
                if (!contact.getPreferredByName().isEmpty()) bodyBuilder.append("<b>Preferred Name: </b>").append(contact.getPreferredByName()).append("<br/>");
                if (!contact.getPrimaryFaxNumber().isEmpty()) bodyBuilder.append("<b>Primary Fax Number: </b>").append(contact.getPrimaryFaxNumber()).append("<br/>");
                if (!contact.getPrimaryTelephoneNumber().isEmpty()) bodyBuilder.append("<b>Primary Telephone Number: </b>").append(contact.getPrimaryTelephoneNumber()).append("<br/>");
                if (!contact.getProfession().isEmpty()) bodyBuilder.append("<b>Profession: </b>").append(contact.getProfession()).append("<br/>");
                if (!contact.getRadioTelephoneNumber().isEmpty()) bodyBuilder.append("<b>Radio Telephone Number: </b>").append(contact.getRadioTelephoneNumber()).append("<br/>");
                if (!contact.getSMTPAddress().isEmpty()) bodyBuilder.append("<b>SMTP Address: </b>").append(contact.getSMTPAddress()).append("<br/>");
                if (!contact.getSpouseName().isEmpty()) bodyBuilder.append("<b>Spouse Name: </b>").append(contact.getSpouseName()).append("<br/>");
                if (!contact.getSurname().isEmpty()) bodyBuilder.append("<b>Surname: </b>").append(contact.getSurname()).append("<br/>");
                if (!contact.getTelexNumber().isEmpty()) bodyBuilder.append("<b>Telex Number: </b>").append(contact.getTelexNumber()).append("<br/>");
                if (!contact.getTitle().isEmpty()) bodyBuilder.append("<b>Title: </b>").append(contact.getTitle()).append("<br/>");
                if (!contact.getTransmittableDisplayName().isEmpty()) bodyBuilder.append("<b>Transmittable Display Name: </b>").append(contact.getTransmittableDisplayName()).append("<br/>");
                if (!contact.getTtytddPhoneNumber().isEmpty()) bodyBuilder.append("<b>TTYTDD Phone Number: </b>").append(contact.getTtytddPhoneNumber()).append("<br/>");
                if (!contact.getWorkAddress().isEmpty()) bodyBuilder.append("<b>Work Address: </b>").append(contact.getWorkAddress()).append("<br/>");
                if (!contact.getWorkAddressCity().isEmpty()) bodyBuilder.append("<b>Work City: </b>").append(contact.getWorkAddressCity()).append("<br/>");
                if (!contact.getWorkAddressCountry().isEmpty()) bodyBuilder.append("<b>Work Country: </b>").append(contact.getWorkAddressCountry()).append("<br/>");
                if (!contact.getWorkAddressPostOfficeBox().isEmpty()) bodyBuilder.append("<b>Work P.O. Box: </b>").append(contact.getWorkAddressPostOfficeBox()).append("<br/>");
                if (!contact.getWorkAddressPostalCode().isEmpty()) bodyBuilder.append("<b>Work Postal Code: </b>").append(contact.getWorkAddressPostalCode()).append("<br/>");
                if (!contact.getWorkAddressState().isEmpty()) bodyBuilder.append("<b>Work State: </b>").append(contact.getWorkAddressState()).append("<br/>");
                if (!contact.getWorkAddressStreet().isEmpty()) bodyBuilder.append("<b>Work Street: </b>").append(contact.getWorkAddressStreet()).append("<br/>");

                try {
                    // A body can be either RTF, HTML or plaintext.
                    String body = "";

                    if (!contact.getRTFBody().isEmpty()) {
                        try {
                            RTFEditorKit rtfParser = new RTFEditorKit();
                            javax.swing.text.Document rtfDocument = rtfParser.createDefaultDocument();
                            rtfParser.read(new ByteArrayInputStream(contact.getRTFBody().getBytes()), rtfDocument, 0);

                            body = rtfDocument.getText(0, rtfDocument.getLength()).replace("\n", "<br/>");
                        } catch (BadLocationException ex) {
                            log.error("Failed to parse RTF: {}", ex.getMessage());
                            log.error("Falling back to un-parsed RTF.");
                            body = contact.getRTFBody();
                        }
                    } else if (!contact.getBodyHTML().isEmpty()) {
                        body = contact.getBodyHTML();
                    } else if (!contact.getBody().isEmpty()) {
                        body = contact.getBody().replace("\n", "<br/>");
                    }

                    bodyBuilder.append("<br/>").append(body);
                } catch (PSTException | IOException ex) {
                    log.error("Failed to get RTF body: {}", ex.getMessage(), ex);
                }

                messageValue.body(bodyBuilder.toString());
                messageValue.headers(contact.getTransportMessageHeaders().replace("\n", "<br/>"));

            } else if (pstObject instanceof PSTAppointment) {
                // Appointment
                PSTAppointment appointment = (PSTAppointment) pstObject;
                List<AttachmentRow> attachments = new ArrayList<>();
                StringBuilder bodyBuilder = new StringBuilder();

                if (!appointment.getLocation().isEmpty()) bodyBuilder.append("<b>Location: </b>").append(appointment.getLocation()).append("<br/>");
                if (!appointment.getAllAttendees().isEmpty()) bodyBuilder.append("<b>All Attendees: </b>").append(appointment.getAllAttendees()).append("<br/>");
                if (!appointment.getToAttendees().isEmpty()) bodyBuilder.append("<b>To Attendees: </b>").append(appointment.getToAttendees()).append("<br/>");
                if (!appointment.getCCAttendees().isEmpty()) bodyBuilder.append("<b>CC Attendees: </b>").append(appointment.getToAttendees()).append("<br/>");
                if (!appointment.getRequiredAttendees().isEmpty()) bodyBuilder.append("<b>Required Attendees: </b>").append(appointment.getRequiredAttendees()).append("<br/>");
                if (appointment.getStartTime() != null) bodyBuilder.append("<b>Start Time: </b>").append(DATE_FORMAT.format(appointment.getStartTime())).append("<br/>");
                if (appointment.getEndTime() != null) bodyBuilder.append("<b>End Time: </b>").append(DATE_FORMAT.format(appointment.getEndTime())).append("<br/>");
                bodyBuilder.append("<b>Recurring: </b>").append(appointment.isRecurring()).append("<br/>");
                if (appointment.isOnlineMeeting()) bodyBuilder.append("<b>Online Meeting: </b>").append(appointment.isOnlineMeeting()).append("<br/>");
                if (!appointment.getNetMeetingOrganizerAlias().isEmpty()) bodyBuilder.append("<b>Organizer Alias: </b>").append(appointment.getNetMeetingOrganizerAlias()).append("<br/>");
                if (!appointment.getNetMeetingDocumentPathName().isEmpty()) bodyBuilder.append("<b>Meeting Document: </b>").append(appointment.getNetMeetingDocumentPathName()).append("<br/>");
                if (!appointment.getNetMeetingServer().isEmpty()) bodyBuilder.append("<b>Meeting Server: </b>").append(appointment.getNetMeetingServer()).append("<br/>");
                if (!appointment.getConferenceServerPassword().isEmpty()) bodyBuilder.append("<b>Conference Server Password: </b>").append(appointment.getConferenceServerPassword()).append("<br/>");

                try {
                    // A body can be either RTF, HTML or plaintext.
                    String body = "";

                    if (!appointment.getRTFBody().isEmpty()) {
                        try {
                            RTFEditorKit rtfParser = new RTFEditorKit();
                            javax.swing.text.Document rtfDocument = rtfParser.createDefaultDocument();
                            rtfParser.read(new ByteArrayInputStream(appointment.getRTFBody().getBytes()), rtfDocument, 0);

                            body = rtfDocument.getText(0, rtfDocument.getLength()).replace("\n", "<br/>");
                        } catch (BadLocationException ex) {
                            log.error("Failed to parse RTF: {}", ex.getMessage());
                            log.error("Falling back to un-parsed RTF.");
                            body = appointment.getRTFBody();
                        }
                    } else if (!appointment.getBodyHTML().isEmpty()) {
                        body = appointment.getBodyHTML();
                    } else if (!appointment.getBody().isEmpty()) {
                        body = appointment.getBody().replace("\n", "<br/>");
                    }

                    bodyBuilder.append("<br/>").append(body);
                } catch (PSTException | IOException ex) {
                    log.error("Failed to get RTF body: {}", ex.getMessage(), ex);
                }

                // Attachments
                for (int i = 0; i < appointment.getNumberOfAttachments(); i++) {
                    PSTAttachment attachment = appointment.getAttachment(i);
                    String attachmentName = attachment.getLongFilename();

                    if (attachmentName.isEmpty()) {
                        // Attempt fallback
                        if (!attachment.getFilename().isEmpty()) {
                            attachmentName = attachment.getFilename();
                        } else {
                            log.error("-----------------------------------------------");
                            log.error("Invalid attachment name! Is there a possible fallback?");
                            log.error("Display name: {}", attachment.getDisplayName());
                            log.error("Long Filename: {}", attachment.getLongFilename());
                            log.error("Filename: {}", attachment.getFilename());
                            log.error("Pathname: {}", attachment.getPathname());
                            log.error("-----------------------------------------------");
                            attachmentName = "ATTACHMENT_NAME_ERROR";
                        }
                    }

                    attachments.add(AttachmentRow.builder()
                            .attachmentName(attachmentName)
                            .lastModificationTime(attachment.getCreationTime())
                            .size(attachment.getSize())
                            .inputStream(attachment.getFileInputStream()).build()
                    );
                }

                messageValue.body(bodyBuilder.toString());
                messageValue.headers(appointment.getTransportMessageHeaders().replace("\n", "<br/>"));
                messageValue.attachments(attachments);

            } else if (pstObject instanceof PSTTask) {
                log.debug("It's a PSTTask!");
            } else if (pstObject instanceof PSTRss) {
                log.debug("It's a PSTRss!");
            } else if (pstObject instanceof PSTActivity) {
                log.debug("It's a PSTActivity!");
            } else if (pstObject instanceof PSTMessage) {
                // Message
                PSTMessage message = (PSTMessage) pstObject;
                String body = "";
                String headers;
                List<AttachmentRow> attachments = new ArrayList<>();

                try {
                    // A body can be either RTF, HTML or plaintext.
                    if (!message.getRTFBody().isEmpty()) {
                        try {
                            RTFEditorKit rtfParser = new RTFEditorKit();
                            javax.swing.text.Document document = rtfParser.createDefaultDocument();
                            rtfParser.read(new ByteArrayInputStream(message.getRTFBody().getBytes()), document, 0);

                            body = document.getText(0, document.getLength()).replace("\n", "<br/>");
                        } catch (BadLocationException ex) {
                            log.error("Failed to parse RTF: {}", ex.getMessage());
                            log.error("Falling back to un-parsed RTF.");
                            body = message.getRTFBody();
                        }
                    } else if (!message.getBodyHTML().isEmpty()) {
                        body = message.getBodyHTML().replace("\n", "<br/>");
                    } else if (!message.getBody().isEmpty()) {
                        body = message.getBody().replace("\n", "<br/>");
                    }
                } catch (PSTException | IOException ex) {
                    log.error("Failed to get RTF body: {}", ex.getMessage(), ex);

                    // Not sure if it's possible for there to be a
                    // HTML/plaintext body as a fallback.
                    log.error("-------------------------------------");
                    log.error("Possibly fallback?");
                    log.error("HTML body: {}", message.getBodyHTML());
                    log.error("Plaintext body: {}", message.getBody());
                    log.error("-------------------------------------");
                }

                // Headers
                headers = message.getTransportMessageHeaders().replace("\n", "<br/>");

                // Attachments
                for (int i = 0; i < message.getNumberOfAttachments(); i++) {
                    PSTAttachment attachment = message.getAttachment(i);
                    String attachmentName = attachment.getLongFilename();

                    if (attachmentName.isEmpty()) {
                        // Attempt fallback
                        if (!attachment.getFilename().isEmpty()) {
                            attachmentName = attachment.getFilename();
                        } else {
                            log.error("-----------------------------------------------");
                            log.error("Invalid attachment name! Is there a possible fallback?");
                            log.error("Display name: {}", attachment.getDisplayName());
                            log.error("Long Filename: {}", attachment.getLongFilename());
                            log.error("Filename: {}", attachment.getFilename());
                            log.error("Pathname: {}", attachment.getPathname());
                            log.error("-----------------------------------------------");
                            attachmentName = "ATTACHMENT_NAME_ERROR";
                        }
                    }

                    attachments.add(AttachmentRow.builder()
                            .attachmentName(attachmentName)
                            .lastModificationTime(attachment.getCreationTime())
                            .size(attachment.getSize())
                            .inputStream(attachment.getFileInputStream()).build()
                    );
                }

                messageValue.body(body);
                messageValue.headers(headers);
                messageValue.attachments(attachments);
            }

            return messageValue.build();
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
            // Exceptions related to the ID.
            log.debug("This ID doesn't belong to the PSTParser.");
        } catch (IOException | PSTException ex) {
            // PST-related exceptions.
            log.error(ex.getMessage(), ex);
        } catch (Exception ex) {
            // Anything unexpected we haven't seen yet.
            log.error("Unknown error: {}", ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public List<MessageRow> getMessagesFromTreeItem(TreeItem<TreeObject> treeItem) {
        if (treeItem.getValue() == null || treeItem.getValue().getFolderID() == null) return new ArrayList<>(0);

        try {
            List<MessageRow> rows = new ArrayList<>();

            // Each PST message is indexed with a "folder_id" field, searching
            // for this field will return every message in a specific folder.
            List<Document> results = searchModel.search(
                    new TermQuery(new Term("folder_id", treeItem.getValue().getFolderID())),
                    Integer.MAX_VALUE
            );

            results.forEach(result -> {
                String messageID = result.get("id");

                rows.add(getMessageRow(messageID));
            });
            return rows;
        } catch (Exception ex) {
            log.warn("That tree item doesn't belong to this parser!");
            log.debug(ex.getMessage(), ex);
            return new ArrayList<>(0);
        }
    }
}
