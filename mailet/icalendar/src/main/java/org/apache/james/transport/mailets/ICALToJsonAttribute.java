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

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.mail.MessagingException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.transport.mailets.model.ICAL;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.GenericMailet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.steveash.guavate.Guavate;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;

import net.fortuna.ical4j.model.Calendar;

/**
 * ICALToJsonAttribute takes a map of ICAL4J objects attached as attribute, and output the map of corresponding json bytes as
 * an other attribute, with unique String keys.
 *
 * The JSON contains the following fields :
 *
 * <ul>
 *     <li><b>ical</b> : the raw ical string, in UTF-8</li>
 *     <li><b>sender</b> : the sender of the mail (compulsory, mail without sender will be discarded)</li>
 *     <li><b>recipient</b> : the recipient of the mail. If the mail have several recipients, each recipient will have
 *     its own JSON.</li>
 *     <li><b>uid</b> : the UID of the ical (optional)</li>
 *     <li><b>sequence</b> : the sequence of the ical (optional)</li>
 *     <li><b>dtstamp</b> : the date stamp of the ical (optional)</li>
 *     <li><b>method</b> : the method of the ical (optional)</li>
 *     <li><b>recurrence-id</b> : the recurrence-id of the ical (optional)</li>
 * </ul>
 *
 * Example are included in test call ICalToJsonAttributeTest.
 *
 *  Configuration example :
 *
 * <pre>
 *     <code>
 *         &lt;mailet matcher=??? class=ICALToJsonAttribute&gt;
 *             &lt;sourceAttribute&gt;icalendars&lt;/sourceAttribute&gt;
 *             &lt;destinationAttribute&gt;icalendarJson&lt;/destinationAttribute&gt;
 *         &lt;/mailet&gt;
 *     </code>
 * </pre>
 */
public class ICALToJsonAttribute extends GenericMailet {

    public static final String SOURCE_ATTRIBUTE_NAME = "source";
    public static final String DESTINATION_ATTRIBUTE_NAME = "destination";
    public static final String DEFAULT_SOURCE_ATTRIBUTE_NAME = "icalendar";
    public static final String DEFAULT_DESTINATION_ATTRIBUTE_NAME = "icalendarJson";

    private static final Logger LOGGER = LoggerFactory.getLogger(ICALToJsonAttribute.class);

    private final ObjectMapper objectMapper;
    private String sourceAttributeName;
    private String destinationAttributeName;

    public ICALToJsonAttribute() {
        this.objectMapper = new ObjectMapper()
            .registerModule(new Jdk8Module());
    }

    public String getSourceAttributeName() {
        return sourceAttributeName;
    }

    public String getDestinationAttributeName() {
        return destinationAttributeName;
    }

    @Override
    public String getMailetInfo() {
        return "ICALToJson Mailet";
    }

    @Override
    public void init() throws MessagingException {
        sourceAttributeName = getInitParameter(SOURCE_ATTRIBUTE_NAME, DEFAULT_SOURCE_ATTRIBUTE_NAME);
        destinationAttributeName = getInitParameter(DESTINATION_ATTRIBUTE_NAME, DEFAULT_DESTINATION_ATTRIBUTE_NAME);
        if (Strings.isNullOrEmpty(sourceAttributeName)) {
            throw new MessagingException(SOURCE_ATTRIBUTE_NAME + " configuration parameter can not be null or empty");
        }
        if (Strings.isNullOrEmpty(destinationAttributeName)) {
            throw new MessagingException(DESTINATION_ATTRIBUTE_NAME + " configuration parameter can not be null or empty");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void service(Mail mail) throws MessagingException {
        if (mail.getAttribute(sourceAttributeName) == null) {
            return;
        }
        if (mail.getSender() == null) {
            return;
        }
        try {
            Map<String, Calendar> calendars = (Map<String, Calendar>) mail.getAttribute(sourceAttributeName);
            Map<String, byte[]> collect = calendars.entrySet()
                .stream()
                .flatMap(calendar -> toJson(calendar, mail))
                .collect(Guavate.toImmutableMap(Pair::getKey, Pair::getValue));
            mail.setAttribute(destinationAttributeName, (Serializable) collect);
        } catch (ClassCastException e) {
            LOGGER.error("Received a mail with {} not being an ICAL object for mail {}", e, sourceAttributeName, mail.getName());
        }
    }

    private Stream<Pair<String, byte[]>> toJson(Map.Entry<String, Calendar> entry, Mail mail) {
        return mail.getRecipients()
            .stream()
            .map(recipient -> toJson(entry.getValue(), recipient, mail.getSender(), mail.getName()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(json -> Pair.of(UUID.randomUUID().toString(), json.getBytes(Charsets.UTF_8)));
    }

    private Optional<String> toJson(Calendar calendar, MailAddress recipient, MailAddress sender, String mailName) {
        try {
            return Optional.of(
                objectMapper.writeValueAsString(ICAL.builder()
                .from(calendar)
                .recipient(recipient)
                .sender(sender)
                .build()));
        } catch (JsonProcessingException e) {
            LOGGER.error("Error while serializing Calendar for mail {}", mailName, e);
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.error("Exception caught while attaching ICAL to the email as JSON for mail {}", mailName, e);
            return Optional.empty();
        }
    }
}
