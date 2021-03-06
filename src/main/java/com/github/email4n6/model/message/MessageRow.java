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
package com.github.email4n6.model.message;

import java.util.Date;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a message row.
 */
@Builder
@EqualsAndHashCode(exclude = {"bookmarked", "tag"})
@ToString
public class MessageRow {

    private @Getter String subject, from, to, cc, id, folderID;
    private @Getter long size;
    private @Getter Date receivedDate;
    private @Getter BooleanProperty bookmarked;
    private @Getter StringProperty tag;
}
