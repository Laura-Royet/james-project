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

package org.apache.james.jmap.methods;

import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.james.jmap.exceptions.MailboxHasChildException;
import org.apache.james.jmap.exceptions.MailboxNameException;
import org.apache.james.jmap.exceptions.MailboxParentNotFoundException;
import org.apache.james.jmap.model.SetError;
import org.apache.james.jmap.model.SetMailboxesRequest;
import org.apache.james.jmap.model.SetMailboxesResponse;
import org.apache.james.jmap.model.SetMailboxesResponse.Builder;
import org.apache.james.jmap.model.mailbox.Mailbox;
import org.apache.james.jmap.model.mailbox.MailboxUpdateRequest;
import org.apache.james.jmap.utils.MailboxUtils;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.exception.MailboxException;
import org.apache.james.mailbox.exception.MailboxNotFoundException;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.mail.model.MailboxId;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;

public class SetMailboxesUpdateProcessor<Id extends MailboxId> implements SetMailboxesProcessor<Id> {

    private final MailboxUtils<Id> mailboxUtils;
    private final MailboxManager mailboxManager;

    @Inject
    @VisibleForTesting
    SetMailboxesUpdateProcessor(MailboxUtils<Id> mailboxUtils, MailboxManager mailboxManager) {
        this.mailboxUtils = mailboxUtils;
        this.mailboxManager = mailboxManager;
    }

    @Override
    public SetMailboxesResponse process(SetMailboxesRequest request, MailboxSession mailboxSession) {
        SetMailboxesResponse.Builder responseBuilder = SetMailboxesResponse.builder();
        request.getUpdate().entrySet().stream().forEach(update -> handleUpdate(update, responseBuilder, mailboxSession));
        return responseBuilder.build();
    }

    private void handleUpdate(Entry<String, MailboxUpdateRequest> update, Builder responseBuilder, MailboxSession mailboxSession) {
        String mailboxId = update.getKey();
        MailboxUpdateRequest updateRequest = update.getValue();
        try {
            validateMailboxName(updateRequest, mailboxSession);
            Mailbox mailbox = getMailbox(mailboxId, mailboxSession);
            validateParent(mailbox, updateRequest, mailboxSession);

            updateMailbox(mailbox, updateRequest, mailboxSession);
            responseBuilder.updated(update.getKey());
        } catch (MailboxNameException e) {
            responseBuilder.notUpdated(mailboxId, SetError.builder()
                    .type("invalidArguments")
                    .description(e.getMessage())
                    .build());
        } catch (MailboxNotFoundException e) {
            responseBuilder.notUpdated(mailboxId, SetError.builder()
                    .type("notFound")
                    .description(String.format("The mailbox '%s' was not found", mailboxId))
                    .build());
        } catch (MailboxParentNotFoundException e) {
            responseBuilder.notUpdated(mailboxId, SetError.builder()
                    .type("notFound")
                    .description(String.format("The parent mailbox '%s' was not found.", e.getParentId()))
                    .build());
        } catch (MailboxHasChildException e) {
            responseBuilder.notUpdated(mailboxId, SetError.builder()
                    .type("invalidArguments")
                    .description("Cannot update a parent mailbox.")
                    .build());
        } catch (MailboxException e) {
            responseBuilder.notUpdated(mailboxId, SetError.builder()
                    .type("anErrorOccurred")
                    .description("An error occurred when updating the mailbox")
                    .build());
        }
    }

    private Mailbox getMailbox(String mailboxId, MailboxSession mailboxSession) throws MailboxNotFoundException {
        return mailboxUtils.mailboxFromMailboxId(mailboxId, mailboxSession)
                .orElseThrow(() -> new MailboxNotFoundException(mailboxId));
    }

    private void validateMailboxName(MailboxUpdateRequest updateRequest, MailboxSession mailboxSession) throws MailboxNameException {
        char pathDelimiter = mailboxSession.getPathDelimiter();
        if (updateRequest.getName()
                .filter(name -> name.contains(String.valueOf(pathDelimiter)))
                .isPresent()) {
            throw new MailboxNameException(String.format("The mailbox '%s' contains an illegal character: '%c'", updateRequest.getName().get(), pathDelimiter));
        }
    }

    private void validateParent(Mailbox mailbox, MailboxUpdateRequest updateRequest, MailboxSession mailboxSession) throws MailboxException, MailboxHasChildException {

        if (updateRequest.getParentId().isPresent()) {
            String newParentId = updateRequest.getParentId().get();
            mailboxUtils.mailboxPathFromMailboxId(newParentId, mailboxSession)
                    .orElseThrow(() -> new MailboxParentNotFoundException(newParentId));
            if (mustChangeParent(mailbox.getParentId(), newParentId) && mailboxUtils.hasChildren(mailbox.getId(), mailboxSession)) {
                throw new MailboxHasChildException();
            }
        }
    }

    private boolean mustChangeParent(Optional<String> currentParentId, String newParentId) {
        return currentParentId
                .map(x -> ! x.equals(newParentId))
                .orElse(true);
    }

    private void updateMailbox(Mailbox mailbox, MailboxUpdateRequest updateRequest, MailboxSession mailboxSession) throws MailboxException {
        MailboxPath originMailboxPath = mailboxUtils.getMailboxPath(mailbox, mailboxSession);
        MailboxPath destinationMailboxPath = computeNewMailboxPath(mailbox, originMailboxPath, updateRequest, mailboxSession);
        if (!originMailboxPath.equals(destinationMailboxPath)) {
            mailboxManager.renameMailbox(originMailboxPath, destinationMailboxPath, mailboxSession);
        }
    }

    private MailboxPath computeNewMailboxPath(Mailbox mailbox, MailboxPath originMailboxPath, MailboxUpdateRequest updateRequest, MailboxSession mailboxSession) {
        MailboxPath modifiedMailboxPath = updateRequest.getName()
                .map(newName -> computeMailboxPathWithNewName(originMailboxPath, newName))
                .orElse(originMailboxPath);
        return updateRequest.getParentId()
                .map(parentMailboxId -> computeMailboxPathWithNewParentId(modifiedMailboxPath, parentMailboxId, mailboxSession))
                .orElse(modifiedMailboxPath);
    }

    private MailboxPath computeMailboxPathWithNewName(MailboxPath originMailboxPath, String newName) {
        return new MailboxPath(originMailboxPath, newName);
    }

    private MailboxPath computeMailboxPathWithNewParentId(MailboxPath originMailboxPath, String parentMailboxId, MailboxSession mailboxSession) {
        Optional<MailboxPath> newParentMailboxPath = mailboxUtils.mailboxPathFromMailboxId(parentMailboxId, mailboxSession);
        String lastName = getCurrentMailboxName(originMailboxPath, mailboxSession);
        return new MailboxPath(originMailboxPath, newParentMailboxPath.get().getName() + mailboxSession.getPathDelimiter() + lastName);
    }

    private String getCurrentMailboxName(MailboxPath originMailboxPath, MailboxSession mailboxSession) {
        List<String> mailboxPathElements = Splitter.on(mailboxSession.getPathDelimiter())
                .splitToList(originMailboxPath.getName());
        return mailboxPathElements.get(mailboxPathElements.size() - 1);
    }

}
