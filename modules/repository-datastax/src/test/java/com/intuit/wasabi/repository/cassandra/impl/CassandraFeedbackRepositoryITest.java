package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.feedbackobjects.UserFeedback;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.accessor.UserFeedbackAccessor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CassandraFeedbackRepositoryITest {

    UserFeedbackAccessor accessor;

    CassandraFeedbackRepository repository;

	private MappingManager manager;

	private String userId = "userid1";

	private Username username;

	private Session session;
    
    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new CassandraRepositoryModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));

        session = injector.getInstance(CassandraDriver.class).getSession();
        manager = new MappingManager(session);
        accessor = manager.createAccessor(UserFeedbackAccessor.class);

    	repository = new CassandraFeedbackRepository(accessor);
        session.execute("delete from wasabi_experiments.user_feedback where user_id = '" + userId + "'");
		username = Username.valueOf(userId);
    }
    
    @After
    public void tearDown() throws Exception {
        session.execute("delete from wasabi_experiments.user_feedback where user_id = '" + userId + "'");
        session.close();
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
