/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.transport.mailets;

import java.util.Map;
import java.util.Optional;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;

public class ICALToHeader extends GenericMailet {
    public static final String ATTRIBUTE_PROPERTY = "attribute";
    public static final String ATTRIBUTE_DEFAULT_NAME = "icalendar";

    public static final String X_MEETING_UID_HEADER = "X-MEETING-UID";
    public static final String X_MEETING_METHOD_HEADER = "X-MEETING-METHOD";
    public static final String X_MEETING_RECURRENCE_ID_HEADER = "X-MEETING-RECURRENCE-ID";
    public static final String X_MEETING_SEQUENCE_HEADER = "X-MEETING-SEQUENCE";
    public static final String X_MEETING_DTSTAMP_HEADER = "X-MEETING-DTSTAMP";

    private static final Logger LOGGER = LoggerFactory.getLogger(ICALToHeader.class);

    private String attribute;

    @Override
    public String getMailetInfo() {
        return "ICALToHeader Mailet";
    }

    @Override
    public void init() throws MessagingException {
        attribute = getInitParameter(ATTRIBUTE_PROPERTY, ATTRIBUTE_DEFAULT_NAME);
        if (Strings.isNullOrEmpty(attribute)) {
            throw new MessagingException("attribute can not be empty or null");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void service(Mail mail) throws MessagingException {
        if (mail.getAttribute(attribute) == null) {
            return;
        }
        try {
            Map<String, Calendar> calendars = (Map<String, Calendar>) mail.getAttribute(attribute);
            Optional<Calendar> calendar = calendars.values()
                .stream()
                .findAny();
            if (calendar.isPresent()) {
                writeToHeaders(calendar.get(), mail.getMessage());
            }
        } catch (ClassCastException e) {
            LOGGER.error("Received a mail with {} not being an ICAL object for mail {}", e, attribute, mail.getName());
        }
    }

    @VisibleForTesting
    String getAttribute() {
        return attribute;
    }

    private void writeToHeaders(Calendar calendar, MimeMessage mimeMessage) throws MessagingException {
        VEvent vevent = (VEvent) calendar.getComponent("VEVENT");
        addIfPresent(mimeMessage, X_MEETING_METHOD_HEADER, calendar.getMethod());
        addIfPresent(mimeMessage, X_MEETING_UID_HEADER, vevent.getUid());
        addIfPresent(mimeMessage, X_MEETING_RECURRENCE_ID_HEADER, vevent.getRecurrenceId());
        addIfPresent(mimeMessage, X_MEETING_SEQUENCE_HEADER, vevent.getSequence());
        addIfPresent(mimeMessage, X_MEETING_DTSTAMP_HEADER, vevent.getDateStamp());
        mimeMessage.saveChanges();
    }

    private void addIfPresent(MimeMessage mimeMessage, String headerName, Property property) {
        if (property != null) {
            try {
                mimeMessage.addHeader(headerName, property.getValue());
            } catch (MessagingException e) {
                LOGGER.warn("Could not add header {} with value {}", headerName, property.getValue());
            }
        }
    }
}
