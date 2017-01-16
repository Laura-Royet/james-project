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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.mailet.Mail;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.apache.mailet.base.test.MimeMessageBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.fortuna.ical4j.model.Calendar;

public class ICalendarParserTest {
    private static final String DEST_ATTRIBUTE = "destAttribute";
    private static final String SOURCE_ATTRIBUTE = "sourceAttribute";

    private static final String DEST_CUSTOM_ATTRIBUTE = "ics.dest.attribute";
    private static final String SOURCE_CUSTOM_ATTRIBUTE = "ics.source.attribute";

    private static final String CONTENT_TRANSFER_ENCODING_VALUE ="8bit";

    private static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String TEXT_CALENDAR_CHARSET_UTF_8 = "text/calendar; charset=utf-8";

    private static final String RIGHT_ICAL_VALUE = "BEGIN:VCALENDAR\n" +
        "END:VCALENDAR";

    private static final String WRONG_ICAL_VALUE = "anyValue";

    private static MimeMessageBuilder.Header[] CALENDAR_HEADERS = {
        new MimeMessageBuilder.Header(CONTENT_TRANSFER_ENCODING, CONTENT_TRANSFER_ENCODING_VALUE),
        new MimeMessageBuilder.Header(CONTENT_TYPE, TEXT_CALENDAR_CHARSET_UTF_8)
    };

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ICalendarParser mailet;

    @Before
    public void setup() throws Exception {
        mailet = new ICalendarParser();
        FakeMailetConfig mci = FakeMailetConfig.builder()
            .mailetName("Test")
            .build();

        mailet.init(mci);
    }

    @Test
    public void serviceShouldNotSetCalendarDataIntoMailAttributeWhenNoSourceAttribute() throws Exception {
        FakeMailetConfig mci = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty(DEST_ATTRIBUTE, DEST_CUSTOM_ATTRIBUTE)
            .build();
        mailet.init(mci);

        Mail mail = FakeMail.builder()
            .build();

        mailet.service(mail);

        assertThat(mail.getAttribute(DEST_CUSTOM_ATTRIBUTE)).isNull();
    }

    @Test
    public void serviceShouldNotSetCalendarDataIntoMailAttributeWhenEmptyICSAttachments() throws Exception {
        FakeMailetConfig mci = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty(SOURCE_ATTRIBUTE, SOURCE_CUSTOM_ATTRIBUTE)
            .setProperty(DEST_ATTRIBUTE, DEST_CUSTOM_ATTRIBUTE)
            .build();
        mailet.init(mci);

        Mail mail = FakeMail.builder()
            .attribute(SOURCE_CUSTOM_ATTRIBUTE, ImmutableMap.of())
            .build();

        mailet.service(mail);

        assertThat(mail.getAttribute(DEST_CUSTOM_ATTRIBUTE)).isNull();
    }

    @Test
    public void serviceShouldNotSetCalendarDataIntoMailAttributeWhenSourceAttributeIsNotAMap() throws Exception {
        FakeMailetConfig mci = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty(SOURCE_ATTRIBUTE, SOURCE_CUSTOM_ATTRIBUTE)
            .setProperty(DEST_ATTRIBUTE, DEST_CUSTOM_ATTRIBUTE)
            .build();
        mailet.init(mci);

        Mail mail = FakeMail.builder()
            .attribute(SOURCE_CUSTOM_ATTRIBUTE, "anyValue")
            .build();

        mailet.service(mail);

        assertThat(mail.getAttribute(DEST_CUSTOM_ATTRIBUTE)).isNull();
    }

    @Test
    public void serviceShouldReturnRightMapOfCalendarWhenRightAttachments() throws Exception {
        FakeMailetConfig mci = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty(SOURCE_ATTRIBUTE, SOURCE_CUSTOM_ATTRIBUTE)
            .setProperty(DEST_ATTRIBUTE, DEST_CUSTOM_ATTRIBUTE)
            .build();
        mailet.init(mci);

        ImmutableMap.Builder attachments = ImmutableMap.builder();
        attachments.put("one", RIGHT_ICAL_VALUE.getBytes());
        Mail mail = FakeMail.builder()
            .attribute(SOURCE_CUSTOM_ATTRIBUTE, attachments.build())
            .build();

        mailet.service(mail);

        Map<String, Calendar> expectedCalendars = (Map<String, Calendar>)mail.getAttribute(DEST_CUSTOM_ATTRIBUTE);
        assertThat(expectedCalendars).hasSize(1);
    }

    @Test
    public void serviceShouldFilterResultWhenErrorParsing() throws Exception {
        FakeMailetConfig mci = FakeMailetConfig.builder()
            .mailetName("Test")
            .setProperty(SOURCE_ATTRIBUTE, SOURCE_CUSTOM_ATTRIBUTE)
            .setProperty(DEST_ATTRIBUTE, DEST_CUSTOM_ATTRIBUTE)
            .build();
        mailet.init(mci);

        ImmutableMap.Builder attachments = ImmutableMap.builder();
        attachments.put("one", WRONG_ICAL_VALUE.getBytes());
        attachments.put("two", RIGHT_ICAL_VALUE.getBytes());
        Mail mail = FakeMail.builder()
            .attribute(SOURCE_CUSTOM_ATTRIBUTE, attachments.build())
            .build();

        mailet.service(mail);

        Map<String, Calendar> expectedCalendars = (Map<String, Calendar>)mail.getAttribute(DEST_CUSTOM_ATTRIBUTE);
        System.out.println(expectedCalendars.get("two"));
        Map.Entry<String, Calendar> expectedCalendar = Maps.immutableEntry("two", new Calendar());

        assertThat(expectedCalendars).hasSize(1)
            .containsExactly(expectedCalendar);
    }

    @Test
    public void getMailetInfoShouldReturn() throws MessagingException {
        ICalendarParser mailet = new ICalendarParser();

        assertThat(mailet.getMailetInfo()).isEqualTo("Calendar Parser");
    }

    private BodyPart createAttachmentBodyPart(String body, String fileName, MimeMessageBuilder.Header... headers) throws MessagingException, IOException {
        return MimeMessageBuilder.bodyPartBuilder()
            .data(body)
            .addHeaders(headers)
            .disposition(MimeBodyPart.ATTACHMENT)
            .filename(fileName)
            .build();
    }
}
