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
package org.apache.james.jmap.model;

import java.util.Map;

import org.apache.james.jmap.methods.Method;
import org.apache.james.jmap.model.mailbox.Mailbox;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class SetMailboxesResponse implements Method.Response {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final ImmutableMap.Builder<MailboxCreationId, Mailbox> created;
        private final ImmutableList.Builder<String> updated;
        private final ImmutableMap.Builder<MailboxCreationId, SetError> notCreated;
        private final ImmutableMap.Builder<String, SetError> notUpdated;

        private Builder() {
            created = ImmutableMap.builder();
            updated = ImmutableList.builder();
            notCreated = ImmutableMap.builder();
            notUpdated = ImmutableMap.builder();
        }

        public Builder creation(MailboxCreationId creationId, Mailbox mailbox) {
            created.put(creationId, mailbox);
            return this;
        }

        public Builder updated(String mailboxId) {
            updated.add(mailboxId);
            return this;
        }

        public Builder notCreated(Map<MailboxCreationId, SetError> notCreated) {
            this.notCreated.putAll(notCreated);
            return this;
        }

        public Builder notCreated(MailboxCreationId mailboxCreationId, SetError setError) {
            this.notCreated.put(mailboxCreationId, setError);
            return this;
        }

        public Builder notUpdated(String mailboxId, SetError setError) {
            notUpdated.put(mailboxId, setError);
            return this;
        }

        public SetMailboxesResponse build() {
            return new SetMailboxesResponse(created.build(), updated.build(), notCreated.build(), notUpdated.build());
        }

    }

    private final ImmutableMap<MailboxCreationId, Mailbox> created;
    private final ImmutableList<String> updated;
    private final ImmutableMap<MailboxCreationId, SetError> notCreated;
    private final ImmutableMap<String, SetError> notUpdated;

    private SetMailboxesResponse(ImmutableMap<MailboxCreationId, Mailbox> created,
            ImmutableList<String> updated,
            ImmutableMap<MailboxCreationId, SetError> notCreated,
            ImmutableMap<String, SetError> notUpdated) {
        this.created = created;
        this.updated = updated;
        this.notCreated = notCreated;
        this.notUpdated = notUpdated;
    }

    public ImmutableMap<MailboxCreationId, Mailbox> getCreated() {
        return created;
    }

    public ImmutableList<String> getUpdated() {
        return updated;
    }

    public Map<MailboxCreationId, SetError> getNotCreated() {
        return notCreated;
    }

    public ImmutableMap<String, SetError> getNotUpdated() {
        return notUpdated;
    }
}
