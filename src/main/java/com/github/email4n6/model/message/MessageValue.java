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

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * The message body, headers etc which get shown when the message row is selected.
 *
 * @author Marten4n6
 */
@Builder
public class MessageValue {

    private @Getter String body, headers;
    private @Getter List<AttachmentRow> attachments;
}
