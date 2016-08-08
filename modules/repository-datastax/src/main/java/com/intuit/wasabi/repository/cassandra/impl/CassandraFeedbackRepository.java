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
package com.intuit.wasabi.repository.cassandra.impl;


import com.datastax.driver.mapping.Result;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.feedbackobjects.UserFeedback;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.FeedbackRepository;
import com.intuit.wasabi.repository.cassandra.accessor.UserFeedbackAccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Cassandra implementation of
 * @see FeedbackRepository
 *
 */
public class CassandraFeedbackRepository implements FeedbackRepository {

    private UserFeedbackAccessor userFeedbackAccessor;

    protected static final Logger LOGGER = LoggerFactory.getLogger(CassandraFeedbackRepository.class);
    
    @Inject
    public CassandraFeedbackRepository(UserFeedbackAccessor userFeedbackAccessor) {
    	this.userFeedbackAccessor = userFeedbackAccessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createUserFeedback(UserFeedback userFeedback) {
        try {

        	userFeedbackAccessor.createUserFeedback(userFeedback.getUsername().getUsername(),
    			userFeedback.getSubmitted(), userFeedback.getScore(), userFeedback.getComments(),
    			userFeedback.isContactOkay(), userFeedback.getEmail());
        }
        catch (Exception e) {
        	LOGGER.error("Error while creating user feedback", e);
            throw new RepositoryException("Could not save feedback from user " + userFeedback, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserFeedback> getUserFeedback(UserInfo.Username username) throws RepositoryException {

        Preconditions.checkNotNull(username, "Parameter \"username\" cannot be null");

        List<UserFeedback> feedbacks = new ArrayList<>();
        try {
        	Result<com.intuit.wasabi.repository.cassandra.pojo.UserFeedback> result = 
        			userFeedbackAccessor.getUserFeedback(username.getUsername());
        	
        	feedbacks = makeFeedbacksFromResult(result);
        }
        catch (Exception e) {
        	LOGGER.error("Error while getting feedback for user " + username.getUsername(), e);
            throw new RepositoryException("Could not retrieve feedback from user " + username, e);
        }
        return feedbacks;
     }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserFeedback> getAllUserFeedback() {

        List<UserFeedback> feedbacks = new ArrayList<>();
        try {
        	Result<com.intuit.wasabi.repository.cassandra.pojo.UserFeedback> result = 
        			userFeedbackAccessor.getAllUserFeedback(); 
        	feedbacks = makeFeedbacksFromResult(result);
        }
        catch (Exception e) {
        	LOGGER.error("Error while getting all user feedback", e);
            throw new RepositoryException("Could not retrieve feedback from all users",e);
        }
        return feedbacks;
    }

	protected List<UserFeedback> makeFeedbacksFromResult(
			Result<com.intuit.wasabi.repository.cassandra.pojo.UserFeedback> result) {
		
		List<UserFeedback> feedbacks = new ArrayList<>();
		for( com.intuit.wasabi.repository.cassandra.pojo.UserFeedback userFeedback : result.all()) {
			UserFeedback feedback = makeUserFeedback(userFeedback);
			
			feedbacks.add(feedback);
		}
		return feedbacks;
	}

	protected UserFeedback makeUserFeedback(
			com.intuit.wasabi.repository.cassandra.pojo.UserFeedback userFeedback) {
		UserFeedback feedback = new UserFeedback();
		feedback.setComments(userFeedback.getComment());
		feedback.setContactOkay(userFeedback.isContactOkay());
		feedback.setEmail(userFeedback.getUserEmail());
		feedback.setScore(userFeedback.getScore());
		feedback.setSubmitted(userFeedback.getSubmitted());
		feedback.setUsername(Username.valueOf(userFeedback.getUserId()));
		return feedback;
	}
 
}
