/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.mapping.MappingManager;
import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.feedbackobjects.UserFeedback;
import com.intuit.wasabi.repository.cassandra.IntegrationTestBase;
import com.intuit.wasabi.repository.cassandra.accessor.UserFeedbackAccessor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CassandraFeedbackRepositoryITest extends IntegrationTestBase {

    UserFeedbackAccessor accessor;

    CassandraFeedbackRepository repository;

    private MappingManager manager;

    private String userId = "userid1";

    private Username username;

    @Before
    public void setUp() throws Exception {
        IntegrationTestBase.setup();

        if (repository != null) return;

        manager = new MappingManager(session);
        accessor = manager.createAccessor(UserFeedbackAccessor.class);

        repository = new CassandraFeedbackRepository(accessor);
        username = Username.valueOf(userId);
    }

    @After
    public void tearDown() throws Exception {
        session.execute("delete from wasabi_experiments.user_feedback where user_id = '" + userId + "'");
    }

    @Test
    public void testCreateAndGetFeedbackSuccess() {
        List<UserFeedback> feedback = repository.getUserFeedback(username);

        assertEquals("Size should be same", 0, feedback.size());

        UserFeedback userFeedback = new UserFeedback();
        userFeedback.setUsername(username);
        userFeedback.setSubmitted(new Date());
        userFeedback.setScore(2);
        userFeedback.setComments("comments1");
        userFeedback.setContactOkay(true);
        userFeedback.setEmail("userId1@example.com");

        repository.createUserFeedback(userFeedback);

        List<UserFeedback> feedbackAfter = repository.getUserFeedback(username);

        assertEquals("Size should be same", 1, feedbackAfter.size());
    }

    @Test
    public void testCreate2AndGetFeedbackSuccess() {
        List<UserFeedback> feedbackAllBefore = repository.getAllUserFeedback();

        List<UserFeedback> feedback = repository.getUserFeedback(username);

        assertEquals("Size should be same", 0, feedback.size());

        UserFeedback userFeedback = new UserFeedback();
        userFeedback.setUsername(username);
        userFeedback.setSubmitted(new Date());
        userFeedback.setScore(2);
        userFeedback.setComments("comments1");
        userFeedback.setContactOkay(true);
        userFeedback.setEmail("userId1@example.com");

        repository.createUserFeedback(userFeedback);

        userFeedback = new UserFeedback();
        userFeedback.setUsername(username);
        userFeedback.setSubmitted(new Date());
        userFeedback.setScore(3);
        userFeedback.setComments("comments2");
        userFeedback.setContactOkay(true);
        userFeedback.setEmail("userId1@example.com");
        repository.createUserFeedback(userFeedback);

        List<UserFeedback> feedbackAfter = repository.getUserFeedback(username);

        assertEquals("Size should be same", 2, feedbackAfter.size());

        List<UserFeedback> feedbackAllAfter = repository.getAllUserFeedback();

        assertEquals("Size should be same", 2 + feedbackAllBefore.size(),
                feedbackAllAfter.size());
    }

}
