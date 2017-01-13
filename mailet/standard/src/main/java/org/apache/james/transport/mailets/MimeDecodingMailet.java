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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.io.IOUtils;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetException;
import org.apache.mailet.base.GenericMailet;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;

/**
 * This mailet decodes a mime attribute values to bytes.
 * <br />
 * It takes only one parameter:
 * <ul>
 * <li>attribute (mandatory): mime content to be decoded, expected to be a Map&lt;String, byte[]&gt;
 * </ul>
 *
 * Then all this map attribute values will be replaced by their content.
 */
public class MimeDecodingMailet extends GenericMailet {

    public static final String ATTRIBUTE_PARAMETER_NAME = "attribute";

    private String attribute;

    @Override
    public void init() throws MessagingException {
        attribute = getInitParameter(ATTRIBUTE_PARAMETER_NAME);
        if (Strings.isNullOrEmpty(attribute)) {
            throw new MailetException("No value for " + ATTRIBUTE_PARAMETER_NAME
                    + " parameter was provided.");
        }
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        if (mail.getAttribute(attribute) == null) {
            return;
        }

        ImmutableMap.Builder<String, byte[]> builder = ImmutableMap.builder();
        for (Map.Entry<String, byte[]> entry: getAttributeContent(mail).entrySet()) {
            builder.put(entry.getKey(), extractContent(entry.getValue()));
        }

        ImmutableMap<String, byte[]> build = builder.build();
        for (Map.Entry<String, byte[]> entry : build.entrySet()) {
            System.out.println(entry.getKey() + "  " + new String(entry.getValue()));
        }
        mail.setAttribute(attribute, build);
    }

    @SuppressWarnings("unchecked")
    private Map<String, byte[]> getAttributeContent(Mail mail) throws MailetException {
        Serializable attributeContent = mail.getAttribute(attribute);
        if (! (attributeContent instanceof Map)) {
            throw new MailetException("Invalid attribute found into attribute "
                    + attribute + "class Map expected but "
                    + attributeContent.getClass() + " found.");
        }
        return (Map<String, byte[]>) attributeContent;
    }

    private byte[] extractContent(byte[] rawMime) throws MessagingException {
        try {
            MimeBodyPart mimeBodyPart = new MimeBodyPart(new ByteArrayInputStream(rawMime));
            return IOUtils.toByteArray(mimeBodyPart.getInputStream());
        } catch (IOException e) {
            throw new MessagingException("Error while extracting content from mime part", e);
        }
    }

    @Override
    public String getMailetInfo() {
        return "MimeDecodingMailet";
    }

}
