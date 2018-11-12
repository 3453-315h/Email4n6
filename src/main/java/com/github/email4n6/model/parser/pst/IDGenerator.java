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

import java.io.IOException;
import java.util.zip.Adler32;

import com.pff.PSTAttachment;
import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTObject;

import lombok.extern.slf4j.Slf4j;

/**
 * Static class for generating IDs relevant to this parser.
 *
 * @author Marten4n6
 */
@Slf4j
final class IDGenerator {

    private IDGenerator() {
        throw new AssertionError("Don't.");
    }

    /**
     * A message ID is split up (with a "-") into two parts: <br/>
     * <ul>
     * <li>The descriptor node ID of the message.</li>
     * <li>The ID of the PSTFile this message belongs to.</li>
     * </ul>
     *
     * @param message The message to generate the ID for.
     * @param pstFileID The ID of the PSTFile this message belongs to.
     * @return The ID of the message.
     */
    static String getID(PSTObject message, String pstFileID) {
        return message.getDescriptorNodeId() + "-" + pstFileID;
    }

    /**
     * @return The ID of the PST folder.
     */
    static String getID(PSTFolder folder) {
        Adler32 id = new Adler32();

        id.update((
                folder.getDisplayName() +
                folder.getDescriptorNodeId()
        ).getBytes());
        return "" + id.getValue();
    }

    /**
     * @return The ID of the PST file.
     */
    static String getID(PSTFile file) {
        try {
            Adler32 id = new Adler32();

            id.update((
                    file.getMessageStore().getTagRecordKeyAsUUID().toString() +
                    file.getRootFolder().getDescriptorNodeId()
            ).getBytes());
            return "" + id.getValue();
        } catch (PSTException | IOException ex) {
            log.error("Failed to generate ID for PSTFile!");
            log.error(ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * @return The ID of the PST attachment.
     */
    static String getID(PSTAttachment attachment, String attachmentName, PSTObject pstObject) {
        Adler32 id = new Adler32();

        id.update((
                pstObject.getDescriptorNodeId() +
                attachmentName +
                attachment.getDescriptorNodeId()
        ).getBytes());
        return "" + id.getValue();
    }
}
