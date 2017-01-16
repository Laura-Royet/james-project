/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                 *
 * *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.transport.mailets;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

import javax.mail.MessagingException;

import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;

import com.google.common.base.Optional;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;

/**
 * <p>
 * This mailet should be after the strip attachment mailet,
 * </p>
 * <p>
 * This mailet should look for ICS and parse it with Ical4J then store it as a mail attribute
 * </p>
 * <p>
 * Configuration:
 * </p>
 * <p>
 *
 * <pre>
 *   &lt;mailet match=&quot;All&quot; class=&quot;ICalendarParser&quot; &gt;
 *     &lt;sourceAttribute&gt;source.attribute.name&lt;/sourceAttribute&gt;  &lt;!-- The attribute which contains output value of StripAttachment mailet -- &gt;
 *     &lt;destAttribute&gt;dest.attribute.name&lt;/destAttribute&gt;  &lt;!-- The attribute store the map of Calendar -- &gt;
 *   &lt;/mailet &gt;
 *
 *   At least one of pattern, notpattern and mimeType is required.
 * </pre>
 *
 * </p>
 */
public class ICalendarParser extends GenericMailet {
    private static final String SOURCE_ATTRIBUTE_PARAMETER_NAME = "sourceAttribute";
    private static final String DEST_ATTRIBUTE_PARAMETER_NAME = "destAttribute";

    private String sourceAttributeName;
    private String destAttributeName;

    @Override
    public void init() throws MessagingException {
        sourceAttributeName = getInitParameter(SOURCE_ATTRIBUTE_PARAMETER_NAME);
        destAttributeName = getInitParameter(DEST_ATTRIBUTE_PARAMETER_NAME);
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        Object icsAttachmentsObj = mail.getAttribute(sourceAttributeName);
        if (icsAttachmentsObj instanceof Map && icsAttachmentsObj != null) {
            Map<String, byte[]> icsAttachments = (Map<String, byte[]>) icsAttachmentsObj;
            Map<String, Calendar> calendars = icsAttachments.entrySet()
                .stream()
                .collect(
                    Collectors.toMap(e -> e.getKey(), e -> createCalendar(e.getValue())))
                .entrySet()
                .stream()
                .filter(optCal -> optCal.getValue().isPresent())
                .collect(
                    Collectors.toMap(p -> p.getKey(), p -> p.getValue().get())
                );
            mail.setAttribute(destAttributeName, (Serializable) calendars);
        }
    }

    /**
     * returns a String describing this mailet.
     *
     * @return A desciption of this mailet
     */
    @Override
    public String getMailetInfo() {
        return "Calendar Parser";
    }

    private Optional<Calendar> createCalendar(byte[] icsContent) {
        CalendarBuilder builder = new CalendarBuilder();
        try {
            return Optional.of(builder.build(new ByteArrayInputStream(icsContent)));
        } catch (IOException e) {
            return Optional.absent();
        } catch (ParserException e) {
            return Optional.absent();
        }
    }
}
