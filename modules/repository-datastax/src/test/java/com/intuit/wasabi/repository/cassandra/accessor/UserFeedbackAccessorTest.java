/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.cassandra.accessor;

import static org.junit.Assert.*;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.pojo.UserFeedback;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

public class UserFeedbackAccessorTest {
    static Session session;
    static MappingManager manager;
    static UserFeedbackAccessor accessor;
    static Mapper<UserFeedback> mapper;
	private static String userId = "userId1";

    @BeforeClass
    public static void setup(){
        Injector injector = Guice.createInjector(new CassandraRepositoryModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));

        session = injector.getInstance(CassandraDriver.class).getSession();
        manager = new MappingManager(session);
        mapper = manager.mapper(UserFeedback.class);
        accessor = manager.createAccessor(UserFeedbackAccessor.class);
        
        session.execute("delete from wasabi_experiments.user_feedback where user_id = '" + userId + "'");
    }

    @Test
    public void testCreateAndGetFeedback(){
    	Date submitted = new Date();
    	int score = 2;
    	String comments = "comments1";
    	boolean contactOk = true;
    	String email = "userId1@example.com";
    	accessor.createUserFeedback(userId, submitted, score, comments, contactOk, email);
    	
    	Result<UserFeedback> result = accessor.getUserFeedback(userId);
    	List<UserFeedback> feedbacks = result.all();
    	assertEquals("Size should be same", 1, feedbacks.size());
    	UserFeedback feedback = feedbacks.get(0);
    	
    	assertEquals("user should be same", userId, feedback.getUserId());
    	assertEquals("submitted should be same", submitted, feedback.getSubmitted());
    	assertEquals("score should be same", score, feedback.getScore());
    	assertEquals("contactOk should be same", contactOk, feedback.isContactOkay());
    	assertEquals("email should be same", email, feedback.getEmail());
    }

}