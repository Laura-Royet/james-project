/****************************************************************
O * Licensed to the Apache Software Foundation (ASF) under one   *
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

package org.apache.james.jmap.model.mailbox;

import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang.NotImplementedException;
import org.apache.james.mailbox.exception.MailboxException;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

@JsonDeserialize(builder = MailboxUpdateRequest.Builder.class)
public class MailboxUpdateRequest {

    public static Builder builder() throws MailboxException {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {

        private Optional<String> id;
        private Optional<String> name;
        private Optional<String> parentId;
        private Optional<Role> role;
        private Optional<SortOrder> sortOrder;

        private Builder() {
            id = Optional.empty();
            name = Optional.empty();
            role = Optional.empty();
            sortOrder = Optional.empty();
            parentId = Optional.empty();
        }

        public Builder id(String id) {
            Preconditions.checkNotNull(id);
            this.id = Optional.of(id);
            return this;
        }

        public Builder name(String name) throws MailboxException {
            Preconditions.checkNotNull(name);
            this.name = Optional.of(name);
            return this;
        }

        public Builder parentId(String parentId) {
            this.parentId = Optional.ofNullable(parentId);
            return this;
        }

        public Builder role(Role role) {
            Preconditions.checkNotNull(role);
            throw new NotImplementedException();
        }

        public Builder sortOrder(SortOrder sortOrder) {
            Preconditions.checkNotNull(sortOrder);
            throw new NotImplementedException();
        }


        public MailboxUpdateRequest build() {
            return new MailboxUpdateRequest(id, name, parentId, role, sortOrder);
        }
    }

    private final Optional<String> id;
    private final Optional<String> name;
    private final Optional<String> parentId;
    private final Optional<Role> role;
    private final Optional<SortOrder> sortOrder;

    @VisibleForTesting
    MailboxUpdateRequest(Optional<String> id, Optional<String> name, Optional<String> parentId, Optional<Role> role, Optional<SortOrder> sortOrder) {

        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.role = role;
        this.sortOrder = sortOrder;
    }

    public Optional<String> getId() {
        return id;
    }

    public Optional<String> getName() {
        return name;
    }

    public Optional<String> getParentId() {
        return parentId;
    }

    public Optional<Role> getRole() {
        return role;
    }

    public Optional<SortOrder> getSortOrder() {
        return sortOrder;
    }


    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof MailboxUpdateRequest) {
            MailboxUpdateRequest other = (MailboxUpdateRequest) obj;
            return Objects.equals(this.id, other.id)
                && Objects.equals(this.name, other.name)
                && Objects.equals(this.parentId, other.parentId)
                && Objects.equals(this.role, other.role)
                && Objects.equals(this.sortOrder, other.sortOrder);
        }
        return false;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(id, name, parentId, role, sortOrder);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(getClass())
                .add("id", id)
                .add("name", name)
                .add("parentId", parentId)
                .add("role", role)
                .add("sortOrder", sortOrder)
                .toString();
    }
}
