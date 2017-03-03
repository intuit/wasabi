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
package com.intuit.wasabi.repository;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.feedbackobjects.UserFeedback;

import java.util.List;

/**
 * Repository for feedback data
 */
public interface FeedbackRepository {

    /**
     * Create user feedback
     *
     * @param userFeedback new user feedback
     */
    void createUserFeedback(UserFeedback userFeedback);

    /**
     * Get user feedback for user
     *
     * @param username current user name
     * @return user feedback list
     */
    List<UserFeedback> getUserFeedback(UserInfo.Username username);

    /**
     * Get all feedbacks
     *
     * @return list of feedbacks
     */
    List<UserFeedback> getAllUserFeedback();
}
