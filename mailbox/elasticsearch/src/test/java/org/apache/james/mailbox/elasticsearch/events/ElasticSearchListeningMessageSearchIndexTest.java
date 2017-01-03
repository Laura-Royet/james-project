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
package org.apache.james.mailbox.elasticsearch.events;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MailboxSession.User;
import org.apache.james.mailbox.MessageUid;
import org.apache.james.mailbox.elasticsearch.ElasticSearchIndexer;
import org.apache.james.mailbox.elasticsearch.ElasticSearchIndexer.UpdatedRepresentation;
import org.apache.james.mailbox.elasticsearch.json.MessageToElasticSearchJson;
import org.apache.james.mailbox.elasticsearch.search.ElasticSearchSearcher;
import org.apache.james.mailbox.model.TestId;
import org.apache.james.mailbox.model.UpdatedFlags;
import org.apache.james.mailbox.store.mail.MessageMapperFactory;
import org.apache.james.mailbox.store.mail.model.Mailbox;
import org.apache.james.mailbox.store.mail.model.MailboxMessage;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ElasticSearchListeningMessageSearchIndexTest {
    
    public static final long MODSEQ = 18L;

    private ElasticSearchIndexer indexer;
    private MessageToElasticSearchJson messageToElasticSearchJson;
    private ElasticSearchListeningMessageSearchIndex testee;
    
    @Before
    public void setup() throws JsonProcessingException {

        MessageMapperFactory mapperFactory = mock(MessageMapperFactory.class);
        messageToElasticSearchJson = mock(MessageToElasticSearchJson.class);
        ElasticSearchSearcher elasticSearchSearcher = mock(ElasticSearchSearcher.class);

        indexer = mock(ElasticSearchIndexer.class);
        
        testee = new ElasticSearchListeningMessageSearchIndex(mapperFactory, indexer, elasticSearchSearcher, messageToElasticSearchJson);
    }
    
    @Test
    public void addShouldIndex() throws Exception {
        //Given
        MailboxSession session = mock(MailboxSession.class);
        MailboxSession.User user = mock(MailboxSession.User.class);
        when(session.getUser())
            .thenReturn(user);
        
        Mailbox mailbox = mock(Mailbox.class);
        MessageUid messageId = MessageUid.of(1);
        TestId mailboxId = TestId.of(12);
        when(mailbox.getMailboxId())
            .thenReturn(mailboxId);
        MailboxMessage message = mockedMessage(messageId);
        List<User> users = ImmutableList.of(user);
        
        String expectedJsonContent = "json content";
        when(messageToElasticSearchJson.convertToJson(eq(message), eq(users)))
            .thenReturn(expectedJsonContent);
        
        //When
        testee.add(session, mailbox, message);
        
        //Then
        verify(indexer).indexMessage(eq("12:1"), eq(expectedJsonContent));
    }
    
        @SuppressWarnings("unchecked")
        @Test
        public void addShouldIndexEmailBodyWhenNotIndexableAttachment() throws Exception {
            //Given
            MailboxSession session = mock(MailboxSession.class);
            MailboxSession.User user = mock(MailboxSession.User.class);
            when(session.getUser())
                .thenReturn(user);
    
            Mailbox mailbox = mock(Mailbox.class);
            MessageUid messageId = MessageUid.of(1);
            TestId mailboxId = TestId.of(12);
            when(mailbox.getMailboxId())
                .thenReturn(mailboxId);
            
            MailboxMessage message = mockedMessage(messageId);
            List<User> users = ImmutableList.of(user);
            
            when(messageToElasticSearchJson.convertToJson(eq(message), eq(users)))
                .thenThrow(JsonProcessingException.class);
            
            String expectedJsonContent = "json content";
            when(messageToElasticSearchJson.convertToJsonWithoutAttachment(eq(message), eq(users)))
                .thenReturn(expectedJsonContent);
            
            //When
            testee.add(session, mailbox, message);
            
            //Then
            verify(indexer).indexMessage(eq("12:1"), eq(expectedJsonContent));
        }

        private MailboxMessage mockedMessage(MessageUid messageId) throws IOException {
            MailboxMessage message = mock(MailboxMessage.class);
            when(message.getUid())
                .thenReturn(messageId);
            return message;
        }

        @SuppressWarnings("unchecked")
        @Test
        public void addShouldNotPropagateExceptionWhenExceptionOccurs() throws Exception {
            //Given
            MailboxSession session = mock(MailboxSession.class);
            MailboxSession.User user = mock(MailboxSession.User.class);
            when(session.getUser())
                .thenReturn(user);
    
            Mailbox mailbox = mock(Mailbox.class);
            MessageUid messageId = MessageUid.of(1);
            TestId mailboxId = TestId.of(12);
            when(mailbox.getMailboxId())
                .thenReturn(mailboxId);
            MailboxMessage message = mockedMessage(messageId);
            List<User> users = ImmutableList.of(user);
            
            when(messageToElasticSearchJson.convertToJson(eq(message), eq(users)))
                .thenThrow(JsonProcessingException.class);
            
            when(messageToElasticSearchJson.convertToJsonWithoutAttachment(eq(message), eq(users)))
                .thenThrow(new JsonGenerationException("expected error"));
            
            //When
            testee.add(session, mailbox, message);
            
            //Then
            //No exception
        }

      @Test
      @SuppressWarnings("unchecked")
      public void deleteShouldWork() throws Exception {
          //Given
          MailboxSession session = mock(MailboxSession.class);
          Mailbox mailbox = mock(Mailbox.class);
          MessageUid messageId = MessageUid.of(1);
          TestId mailboxId = TestId.of(12);
          when(mailbox.getMailboxId())
              .thenReturn(mailboxId);
          
          BulkResponse expectedBulkResponse = mock(BulkResponse.class);
          when(indexer.deleteMessages(any(List.class)))
              .thenReturn(expectedBulkResponse);
          
          //When
          testee.delete(session, mailbox, Lists.newArrayList(messageId));
          
          //Then
          verify(indexer).deleteMessages(eq(Lists.newArrayList("12:1")));
      }

        @Test
        @SuppressWarnings("unchecked")
        public void deleteShouldWorkWhenMultipleMessageIds() throws Exception {
            //Given
            MailboxSession session = mock(MailboxSession.class);
            Mailbox mailbox = mock(Mailbox.class);
            MessageUid messageId1 = MessageUid.of(1);
            MessageUid messageId2 = MessageUid.of(2);
            MessageUid messageId3 = MessageUid.of(3);
            MessageUid messageId4 = MessageUid.of(4);
            MessageUid messageId5 = MessageUid.of(5);
            TestId mailboxId = TestId.of(12);
            when(mailbox.getMailboxId())
                .thenReturn(mailboxId);
    
            BulkResponse expectedBulkResponse = mock(BulkResponse.class);
            when(indexer.deleteMessages(any(List.class)))
                .thenReturn(expectedBulkResponse);
            
            //When
            testee.delete(session, mailbox, Lists.newArrayList(messageId1, messageId2, messageId3, messageId4, messageId5));
            
            //Then
            verify(indexer).deleteMessages(eq(Lists.newArrayList("12:1", "12:2", "12:3", "12:4", "12:5")));
        }

        @Test
        @SuppressWarnings("unchecked")
        public void deleteShouldNotPropagateExceptionWhenExceptionOccurs() throws Exception {
            //Given
            MailboxSession session = mock(MailboxSession.class);
            Mailbox mailbox = mock(Mailbox.class);
            MessageUid messageId = MessageUid.of(1);
            TestId mailboxId = TestId.of(12);
            when(mailbox.getMailboxId())
                .thenReturn(mailboxId);
            
            when(indexer.deleteMessages(any(List.class)))
                .thenThrow(new ElasticsearchException(""));
            
            //When
            testee.delete(session, mailbox, Lists.newArrayList(messageId));
            
            //Then
            //No exception
        }

        @Test
        public void updateShouldWork() throws Exception {
            //Given
            MailboxSession session = mock(MailboxSession.class);
            Mailbox mailbox = mock(Mailbox.class);
            Flags flags = new Flags();
            MessageUid messageId = MessageUid.of(1);
            UpdatedFlags updatedFlags = new UpdatedFlags(messageId, MODSEQ, flags, flags);
            TestId mailboxId = TestId.of(12);
            
            when(mailbox.getMailboxId())
                .thenReturn(mailboxId);
            
            when(messageToElasticSearchJson.getUpdatedJsonMessagePart(any(Flags.class), any(Long.class)))
                .thenReturn("json updated content");
            
            //When
            testee.update(session, mailbox, Lists.newArrayList(updatedFlags));
            
            //Then
            ImmutableList<UpdatedRepresentation> expectedUpdatedRepresentations = ImmutableList.of(new UpdatedRepresentation(mailboxId.serialize() + ":" + messageId.asLong(), "json updated content"));
            verify(indexer).updateMessages(expectedUpdatedRepresentations);
        }

        @Test
        public void updateShouldNotPropagateExceptionWhenExceptionOccurs() throws Exception {
            //Given
            MailboxSession session = mock(MailboxSession.class);
            Mailbox mailbox = mock(Mailbox.class);
            Flags flags = new Flags();
            MessageUid messageId = MessageUid.of(1);
            UpdatedFlags updatedFlags = new UpdatedFlags(messageId, MODSEQ, flags, flags);
            TestId mailboxId = TestId.of(12);
    
            when(mailbox.getMailboxId())
                .thenReturn(mailboxId);

            ImmutableList<UpdatedRepresentation> expectedUpdatedRepresentations = ImmutableList.of(new UpdatedRepresentation(mailboxId.serialize() + ":" + messageId.asLong(), "json updated content"));
            when(indexer.updateMessages(expectedUpdatedRepresentations))
                .thenThrow(new ElasticsearchException(""));
            
            //When
            testee.update(session, mailbox, Lists.newArrayList(updatedFlags));
            
            //Then
            //No exception
        }

        @Test
        public void deleteAllShouldWork() throws Exception {
            //Given
            MailboxSession session = mock(MailboxSession.class);
            Mailbox mailbox = mock(Mailbox.class);
            TestId mailboxId = TestId.of(12);
    
            when(mailbox.getMailboxId())
                .thenReturn(mailboxId);
    
            //When
            testee.deleteAll(session, mailbox);
            
            //Then
            QueryBuilder expectedQueryBuilder = QueryBuilders.termQuery("mailboxId", "12");
            verify(indexer).deleteAllMatchingQuery(refEq(expectedQueryBuilder));
        }

        @Test
        public void deleteAllShouldNotPropagateExceptionWhenExceptionOccurs() throws Exception {
            //Given
            MailboxSession session = mock(MailboxSession.class);
            Mailbox mailbox = mock(Mailbox.class);
            TestId mailboxId = TestId.of(12);
    
            when(mailbox.getMailboxId())
                .thenReturn(mailboxId);
       
            doThrow(RuntimeException.class)
                .when(indexer).deleteAllMatchingQuery(QueryBuilders.termQuery("mailboxId", "12"));
    
            //When
            testee.deleteAll(session, mailbox);
            
            //Then
            //No Exception
        }

}
