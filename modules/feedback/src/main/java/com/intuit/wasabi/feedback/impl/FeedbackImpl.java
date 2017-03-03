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
package com.intuit.wasabi.feedback.impl;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.feedback.Feedback;
import com.intuit.wasabi.feedbackobjects.UserFeedback;
import com.intuit.wasabi.repository.FeedbackRepository;
import com.intuit.wasabi.userdirectory.UserDirectory;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public class FeedbackImpl implements Feedback {

    private final FeedbackRepository feedbackRepository;
    private UserDirectory userDirectory;
    private static final Logger LOGGER = getLogger(FeedbackImpl.class);

    @Inject
    public FeedbackImpl(final FeedbackRepository feedbackRepository, final UserDirectory userDirectory) {
        super();

        this.feedbackRepository = feedbackRepository;
        this.userDirectory = userDirectory;
    }

    @Override
    public void createUserFeedback(UserFeedback userFeedback) {
        checkNotNull(userFeedback.getUsername(), "Parameter \"username\" cannot be null");
        checkNotNull(userFeedback.getSubmitted(), "Parameter \"submitted\" cannot be null");

        if (userFeedback.getScore() == 0 && "".equals(userFeedback.getComments()) && !userFeedback.isContactOkay()) {
            throw new IllegalArgumentException("error, one of score, contactOkay, or comments must be provided");
        }

        if (userFeedback.isContactOkay()) {
            userFeedback.setEmail(userDirectory.lookupUser(userFeedback.getUsername()).getEmail());
        }

        LOGGER.debug("User Feedback: storing feedback from user " + userFeedback.getUsername().toString() + "submitted " +
                "at time " + userFeedback.getSubmitted().toString());
        feedbackRepository.createUserFeedback(userFeedback);
    }

    @Override
    public List<UserFeedback> getUserFeedback(UserInfo.Username username) {
        checkNotNull(username, "Parameter \"username\" cannot be null");
        return feedbackRepository.getUserFeedback(username);
    }

    @Override
    public List<UserFeedback> getAllUserFeedback() {
        return feedbackRepository.getAllUserFeedback();
    }
}
