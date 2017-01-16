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

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import javax.mail.MessagingException;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.MailAddressFixture;
import org.apache.mailet.base.test.FakeMail;
import org.apache.mailet.base.test.FakeMailetConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableMap;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.model.Calendar;

public class ICALToJsonAttributeTest {
    public static final MailAddress SENDER = MailAddressFixture.ANY_AT_JAMES;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ICALToJsonAttribute testee;

    @Before
    public void setUp() {
        testee = new ICALToJsonAttribute();
    }

    @Test
    public void getMailetInfoShouldReturnExpectedValue() throws Exception {
        assertThat(testee.getMailetInfo()).isEqualTo("ICALToJson Mailet");
    }

    @Test
    public void initShouldSetAttributesWhenAbsent() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        assertThat(testee.getSourceAttributeName()).isEqualTo(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME);
        assertThat(testee.getDestinationAttributeName()).isEqualTo(ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME);
    }

    @Test
    public void initShouldThrowOnEmptySourceAttribute() throws Exception {
        expectedException.expect(MessagingException.class);

        testee.init(FakeMailetConfig.builder()
            .setProperty(ICALToJsonAttribute.SOURCE_ATTRIBUTE_NAME, "")
            .build());
    }

    @Test
    public void initShouldThrowOnEmptyDestinationAttribute() throws Exception {
        expectedException.expect(MessagingException.class);

        testee.init(FakeMailetConfig.builder()
            .setProperty(ICALToJsonAttribute.DESTINATION_ATTRIBUTE_NAME, "")
            .build());
    }

    @Test
    public void initShouldSetAttributesWhenPresent() throws Exception {
        String destination = "myDestination";
        String source = "mySource";
        testee.init(FakeMailetConfig.builder()
            .setProperty(ICALToJsonAttribute.SOURCE_ATTRIBUTE_NAME, source)
            .setProperty(ICALToJsonAttribute.DESTINATION_ATTRIBUTE_NAME, destination)
            .build());

        assertThat(testee.getSourceAttributeName()).isEqualTo(source);
        assertThat(testee.getDestinationAttributeName()).isEqualTo(destination);
    }

    @Test
    public void serviceShouldFilterMailsWithoutICALs() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(MailAddressFixture.OTHER_AT_JAMES)
            .build();
        testee.service(mail);

        assertThat(mail.getAttribute(ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME))
            .isNull();
    }

    @Test
    public void serviceShouldNotFailOnWrongAttributeType() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(MailAddressFixture.OTHER_AT_JAMES)
            .attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, "wrong type")
            .build();
        testee.service(mail);

        assertThat(mail.getAttribute(ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME))
            .isNull();
    }

    @Test
    public void serviceShouldNotFailOnWrongAttributeParameter() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        ImmutableMap<String, String> wrongParametrizedMap = ImmutableMap.<String, String>builder()
            .put("key", "value")
            .build();
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(MailAddressFixture.OTHER_AT_JAMES)
            .attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, wrongParametrizedMap)
            .build();
        testee.service(mail);

        assertThat(mail.getAttribute(ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME))
            .isNull();
    }

    @Test
    public void serviceShouldFilterMailsWithoutSender() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        Calendar calendar = new CalendarBuilder().build(ClassLoader.getSystemResourceAsStream("ics/meeting.ics"));
        ImmutableMap<String, Calendar> icals = ImmutableMap.<String, Calendar>builder()
            .put("key", calendar)
            .build();
        Mail mail = FakeMail.builder()
            .recipient(MailAddressFixture.OTHER_AT_JAMES)
            .attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, icals)
            .build();
        testee.service(mail);

        assertThat(mail.getAttribute(ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME))
            .isNull();
    }


    @Test
    public void serviceShouldAttachEmptyListWhenNoRecipient() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        Calendar calendar = new CalendarBuilder().build(ClassLoader.getSystemResourceAsStream("ics/meeting.ics"));
        ImmutableMap<String, Calendar> icals = ImmutableMap.<String, Calendar>builder()
            .put("key", calendar)
            .build();
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, icals)
            .build();
        testee.service(mail);

        assertThat((List) mail.getAttribute(ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME))
            .isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serviceShouldAttachJson() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        Calendar calendar = new CalendarBuilder().build(ClassLoader.getSystemResourceAsStream("ics/meeting.ics"));
        ImmutableMap<String, Calendar> icals = ImmutableMap.<String, Calendar>builder()
            .put("key", calendar)
            .build();
        MailAddress recipient = MailAddressFixture.ANY_AT_JAMES2;
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(recipient)
            .attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, icals)
            .build();
        testee.service(mail);

        List<String> jsons = (List<String>) mail.getAttribute(ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME);
        assertThat(jsons).hasSize(1);
        assertThatJson(jsons.get(0)).isEqualTo("{\n" +
            "\t\"ical\": \"BEGIN:VCALENDAR\\r\\nPRODID:-//Aliasource Groupe LINAGORA//OBM Calendar 3.2.1-rc2//FR\\r\\nCALSCALE:GREGORIAN\\r\\nX-OBM-TIME:1483703436\\r\\nVERSION:2.0\\r\\nMETHOD:REQUEST\\r\\nBEGIN:VEVENT\\r\\nCREATED:20170106T115035Z\\r\\nLAST-MODIFIED:20170106T115036Z\\r\\nDTSTAMP:20170106T115036Z\\r\\nDTSTART:20170111T090000Z\\r\\nDURATION:PT1H30M\\r\\nTRANSP:OPAQUE\\r\\nSEQUENCE:0\\r\\nSUMMARY:Sprint planning #23\\r\\nDESCRIPTION:\\r\\nCLASS:PUBLIC\\r\\nPRIORITY:5\\r\\nORGANIZER;X-OBM-ID=128;CN=Raphael OUAZANA:MAILTO:ouazana@linagora.com\\r\\nX-OBM-DOMAIN:linagora.com\\r\\nX-OBM-DOMAIN-UUID:02874f7c-d10e-102f-acda-0015176f7922\\r\\nLOCATION:Hangout\\r\\nCATEGORIES:\\r\\nX-OBM-COLOR:\\r\\nUID:f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Matthieu EXT_BAECHLER;PARTSTAT=NEEDS-ACTION;X-OBM-ID=302:MAILTO:baechler@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Laura ROYET;PARTSTAT=NEEDS-ACTION;X-OBM-ID=723:MAILTO:royet@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Raphael OUAZANA;PARTSTAT=ACCEPTED;X-OBM-ID=128:MAILTO:ouazana@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Luc DUZAN;PARTSTAT=NEEDS-ACTION;X-OBM-ID=715:MAILTO:duzan@linagora.com\\r\\nATTENDEE;CUTYPE=RESOURCE;CN=Salle de reunion Lyon;PARTSTAT=ACCEPTED;X-OBM-ID=66:MAILTO:noreply@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Antoine DUPRAT;PARTSTAT=NEEDS-ACTION;X-OBM-ID=453:MAILTO:duprat@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=\\\"Benoît TELLIER\\\";PARTSTAT=NEEDS-ACTION;X-OBM-ID=623:MAILTO:tellier@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Quynh Quynh N NGUYEN;PARTSTAT=NEEDS-ACTION;X-OBM-ID=769:MAILTO:nguyen@linagora.com\\r\\nEND:VEVENT\\r\\nEND:VCALENDAR\\r\\n\",\n" +
            "\t\"sender\": \"" + SENDER.asString() + "\",\n" +
            "\t\"recipient\": \"" + recipient.asString() + "\",\n" +
            "\t\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\",\n" +
            "\t\"sequence\": \"0\",\n" +
            "\t\"dtstamp\": \"20170106T115036Z\",\n" +
            "\t\"method\": \"REQUEST\",\n" +
            "\t\"recurrence-id\": null\n" +
            "}");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serviceShouldAttachJsonForSeveralRecipient() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        Calendar calendar = new CalendarBuilder().build(ClassLoader.getSystemResourceAsStream("ics/meeting.ics"));
        ImmutableMap<String, Calendar> icals = ImmutableMap.<String, Calendar>builder()
            .put("key", calendar)
            .build();
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipients(MailAddressFixture.OTHER_AT_JAMES, MailAddressFixture.ANY_AT_JAMES2)
            .attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, icals)
            .build();
        testee.service(mail);

        List<String> jsons = (List<String>) mail.getAttribute(ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME);
        assertThat(jsons).hasSize(2);
        assertThatJson(jsons.get(0)).isEqualTo("{\n" +
            "\t\"ical\": \"BEGIN:VCALENDAR\\r\\nPRODID:-//Aliasource Groupe LINAGORA//OBM Calendar 3.2.1-rc2//FR\\r\\nCALSCALE:GREGORIAN\\r\\nX-OBM-TIME:1483703436\\r\\nVERSION:2.0\\r\\nMETHOD:REQUEST\\r\\nBEGIN:VEVENT\\r\\nCREATED:20170106T115035Z\\r\\nLAST-MODIFIED:20170106T115036Z\\r\\nDTSTAMP:20170106T115036Z\\r\\nDTSTART:20170111T090000Z\\r\\nDURATION:PT1H30M\\r\\nTRANSP:OPAQUE\\r\\nSEQUENCE:0\\r\\nSUMMARY:Sprint planning #23\\r\\nDESCRIPTION:\\r\\nCLASS:PUBLIC\\r\\nPRIORITY:5\\r\\nORGANIZER;X-OBM-ID=128;CN=Raphael OUAZANA:MAILTO:ouazana@linagora.com\\r\\nX-OBM-DOMAIN:linagora.com\\r\\nX-OBM-DOMAIN-UUID:02874f7c-d10e-102f-acda-0015176f7922\\r\\nLOCATION:Hangout\\r\\nCATEGORIES:\\r\\nX-OBM-COLOR:\\r\\nUID:f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Matthieu EXT_BAECHLER;PARTSTAT=NEEDS-ACTION;X-OBM-ID=302:MAILTO:baechler@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Laura ROYET;PARTSTAT=NEEDS-ACTION;X-OBM-ID=723:MAILTO:royet@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Raphael OUAZANA;PARTSTAT=ACCEPTED;X-OBM-ID=128:MAILTO:ouazana@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Luc DUZAN;PARTSTAT=NEEDS-ACTION;X-OBM-ID=715:MAILTO:duzan@linagora.com\\r\\nATTENDEE;CUTYPE=RESOURCE;CN=Salle de reunion Lyon;PARTSTAT=ACCEPTED;X-OBM-ID=66:MAILTO:noreply@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Antoine DUPRAT;PARTSTAT=NEEDS-ACTION;X-OBM-ID=453:MAILTO:duprat@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=\\\"Benoît TELLIER\\\";PARTSTAT=NEEDS-ACTION;X-OBM-ID=623:MAILTO:tellier@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Quynh Quynh N NGUYEN;PARTSTAT=NEEDS-ACTION;X-OBM-ID=769:MAILTO:nguyen@linagora.com\\r\\nEND:VEVENT\\r\\nEND:VCALENDAR\\r\\n\",\n" +
            "\t\"sender\": \"" + SENDER.asString() + "\",\n" +
            "\t\"recipient\": \"" + MailAddressFixture.OTHER_AT_JAMES.asString() + "\",\n" +
            "\t\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\",\n" +
            "\t\"sequence\": \"0\",\n" +
            "\t\"dtstamp\": \"20170106T115036Z\",\n" +
            "\t\"method\": \"REQUEST\",\n" +
            "\t\"recurrence-id\": null\n" +
            "}");
        assertThatJson(jsons.get(1)).isEqualTo("{\n" +
            "\t\"ical\": \"BEGIN:VCALENDAR\\r\\nPRODID:-//Aliasource Groupe LINAGORA//OBM Calendar 3.2.1-rc2//FR\\r\\nCALSCALE:GREGORIAN\\r\\nX-OBM-TIME:1483703436\\r\\nVERSION:2.0\\r\\nMETHOD:REQUEST\\r\\nBEGIN:VEVENT\\r\\nCREATED:20170106T115035Z\\r\\nLAST-MODIFIED:20170106T115036Z\\r\\nDTSTAMP:20170106T115036Z\\r\\nDTSTART:20170111T090000Z\\r\\nDURATION:PT1H30M\\r\\nTRANSP:OPAQUE\\r\\nSEQUENCE:0\\r\\nSUMMARY:Sprint planning #23\\r\\nDESCRIPTION:\\r\\nCLASS:PUBLIC\\r\\nPRIORITY:5\\r\\nORGANIZER;X-OBM-ID=128;CN=Raphael OUAZANA:MAILTO:ouazana@linagora.com\\r\\nX-OBM-DOMAIN:linagora.com\\r\\nX-OBM-DOMAIN-UUID:02874f7c-d10e-102f-acda-0015176f7922\\r\\nLOCATION:Hangout\\r\\nCATEGORIES:\\r\\nX-OBM-COLOR:\\r\\nUID:f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Matthieu EXT_BAECHLER;PARTSTAT=NEEDS-ACTION;X-OBM-ID=302:MAILTO:baechler@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Laura ROYET;PARTSTAT=NEEDS-ACTION;X-OBM-ID=723:MAILTO:royet@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Raphael OUAZANA;PARTSTAT=ACCEPTED;X-OBM-ID=128:MAILTO:ouazana@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Luc DUZAN;PARTSTAT=NEEDS-ACTION;X-OBM-ID=715:MAILTO:duzan@linagora.com\\r\\nATTENDEE;CUTYPE=RESOURCE;CN=Salle de reunion Lyon;PARTSTAT=ACCEPTED;X-OBM-ID=66:MAILTO:noreply@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Antoine DUPRAT;PARTSTAT=NEEDS-ACTION;X-OBM-ID=453:MAILTO:duprat@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=\\\"Benoît TELLIER\\\";PARTSTAT=NEEDS-ACTION;X-OBM-ID=623:MAILTO:tellier@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Quynh Quynh N NGUYEN;PARTSTAT=NEEDS-ACTION;X-OBM-ID=769:MAILTO:nguyen@linagora.com\\r\\nEND:VEVENT\\r\\nEND:VCALENDAR\\r\\n\",\n" +
            "\t\"sender\": \"" + SENDER.asString() + "\",\n" +
            "\t\"recipient\": \"" + MailAddressFixture.ANY_AT_JAMES2.asString() + "\",\n" +
            "\t\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\",\n" +
            "\t\"sequence\": \"0\",\n" +
            "\t\"dtstamp\": \"20170106T115036Z\",\n" +
            "\t\"method\": \"REQUEST\",\n" +
            "\t\"recurrence-id\": null\n" +
            "}");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void serviceShouldAttachJsonForSeveralICALs() throws Exception {
        testee.init(FakeMailetConfig.builder().build());

        Calendar calendar = new CalendarBuilder().build(ClassLoader.getSystemResourceAsStream("ics/meeting.ics"));
        Calendar calendar2 = new CalendarBuilder().build(ClassLoader.getSystemResourceAsStream("ics/meeting_2.ics"));
        ImmutableMap<String, Calendar> icals = ImmutableMap.<String, Calendar>builder()
            .put("key", calendar)
            .put("key2", calendar2)
            .build();
        MailAddress recipient = MailAddressFixture.OTHER_AT_JAMES;
        Mail mail = FakeMail.builder()
            .sender(SENDER)
            .recipient(recipient)
            .attribute(ICALToJsonAttribute.DEFAULT_SOURCE_ATTRIBUTE_NAME, icals)
            .build();
        testee.service(mail);

        List<String> jsons = (List<String>) mail.getAttribute(ICALToJsonAttribute.DEFAULT_DESTINATION_ATTRIBUTE_NAME);
        assertThat(jsons).hasSize(2);
        assertThatJson(jsons.get(0)).isEqualTo("{\n" +
            "\t\"ical\": \"BEGIN:VCALENDAR\\r\\nPRODID:-//Aliasource Groupe LINAGORA//OBM Calendar 3.2.1-rc2//FR\\r\\nCALSCALE:GREGORIAN\\r\\nX-OBM-TIME:1483703436\\r\\nVERSION:2.0\\r\\nMETHOD:REQUEST\\r\\nBEGIN:VEVENT\\r\\nCREATED:20170106T115035Z\\r\\nLAST-MODIFIED:20170106T115036Z\\r\\nDTSTAMP:20170106T115036Z\\r\\nDTSTART:20170111T090000Z\\r\\nDURATION:PT1H30M\\r\\nTRANSP:OPAQUE\\r\\nSEQUENCE:0\\r\\nSUMMARY:Sprint planning #23\\r\\nDESCRIPTION:\\r\\nCLASS:PUBLIC\\r\\nPRIORITY:5\\r\\nORGANIZER;X-OBM-ID=128;CN=Raphael OUAZANA:MAILTO:ouazana@linagora.com\\r\\nX-OBM-DOMAIN:linagora.com\\r\\nX-OBM-DOMAIN-UUID:02874f7c-d10e-102f-acda-0015176f7922\\r\\nLOCATION:Hangout\\r\\nCATEGORIES:\\r\\nX-OBM-COLOR:\\r\\nUID:f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Matthieu EXT_BAECHLER;PARTSTAT=NEEDS-ACTION;X-OBM-ID=302:MAILTO:baechler@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Laura ROYET;PARTSTAT=NEEDS-ACTION;X-OBM-ID=723:MAILTO:royet@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Raphael OUAZANA;PARTSTAT=ACCEPTED;X-OBM-ID=128:MAILTO:ouazana@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Luc DUZAN;PARTSTAT=NEEDS-ACTION;X-OBM-ID=715:MAILTO:duzan@linagora.com\\r\\nATTENDEE;CUTYPE=RESOURCE;CN=Salle de reunion Lyon;PARTSTAT=ACCEPTED;X-OBM-ID=66:MAILTO:noreply@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Antoine DUPRAT;PARTSTAT=NEEDS-ACTION;X-OBM-ID=453:MAILTO:duprat@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=\\\"Benoît TELLIER\\\";PARTSTAT=NEEDS-ACTION;X-OBM-ID=623:MAILTO:tellier@linagora.com\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Quynh Quynh N NGUYEN;PARTSTAT=NEEDS-ACTION;X-OBM-ID=769:MAILTO:nguyen@linagora.com\\r\\nEND:VEVENT\\r\\nEND:VCALENDAR\\r\\n\",\n" +
            "\t\"sender\": \"" + SENDER.asString() + "\",\n" +
            "\t\"recipient\": \"" + recipient.asString() + "\",\n" +
            "\t\"uid\": \"f1514f44bf39311568d640727cff54e819573448d09d2e5677987ff29caa01a9e047feb2aab16e43439a608f28671ab7c10e754ce92be513f8e04ae9ff15e65a9819cf285a6962bc\",\n" +
            "\t\"sequence\": \"0\",\n" +
            "\t\"dtstamp\": \"20170106T115036Z\",\n" +
            "\t\"method\": \"REQUEST\",\n" +
            "\t\"recurrence-id\": null\n" +
            "}");
        assertThatJson(jsons.get(1)).isEqualTo("{\n" +
            "\t\"ical\": \"BEGIN:VCALENDAR\\r\\nPRODID:-//Aliasource Groupe LINAGORA//OBM Calendar 3.2.1-rc2//FR\\r\\nCALSCALE:GREGORIAN\\r\\nX-OBM-TIME:1483439571\\r\\nVERSION:2.0\\r\\nMETHOD:REQUEST\\r\\nBEGIN:VEVENT\\r\\nCREATED:20170103T103250Z\\r\\nLAST-MODIFIED:20170103T103250Z\\r\\nDTSTAMP:20170103T103250Z\\r\\nDTSTART:20170120T100000Z\\r\\nDURATION:PT30M\\r\\nTRANSP:OPAQUE\\r\\nSEQUENCE:0\\r\\nSUMMARY:Sprint Social #3 Demo\\r\\nDESCRIPTION:\\r\\nCLASS:PUBLIC\\r\\nPRIORITY:5\\r\\nORGANIZER;X-OBM-ID=468;CN=Christophe HAMERLING:MAILTO:chamerling@linagora.com\\r\\nX-OBM-DOMAIN:linagora.com\\r\\nX-OBM-DOMAIN-UUID:02874f7c-d10e-102f-acda-0015176f7922\\r\\nLOCATION:hangout\\r\\nCATEGORIES:\\r\\nX-OBM-COLOR:\\r\\nUID:f1514f44bf39311568d64072ac247c17656ceafde3b4b3eba961c8c5184cdc6ee047feb2aab16e43439a608f28671ab7c10e754c301b1e32001ad51dd20eac2fc7af20abf4093bbe\\r\\nATTENDEE;CUTYPE=INDIVIDUAL;RSVP=TRUE;CN=Michael BAILLY;PARTSTAT=NEEDS-ACTION;X-OBM-ID=348:MAILTO:mbailly@linagora.com\\r\\nEND:VEVENT\\r\\nEND:VCALENDAR\\r\\n\",\n" +
            "\t\"sender\": \"" + SENDER.asString() + "\",\n" +
            "\t\"recipient\": \"" + recipient.asString() + "\",\n" +
            "\t\"uid\": \"f1514f44bf39311568d64072ac247c17656ceafde3b4b3eba961c8c5184cdc6ee047feb2aab16e43439a608f28671ab7c10e754c301b1e32001ad51dd20eac2fc7af20abf4093bbe\",\n" +
            "\t\"sequence\": \"0\",\n" +
            "\t\"dtstamp\": \"20170103T103250Z\",\n" +
            "\t\"method\": \"REQUEST\",\n" +
            "\t\"recurrence-id\": null\n" +
            "}");
    }
}
