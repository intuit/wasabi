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
package com.intuit.wasabi.feedback;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.feedbackobjects.UserFeedback;

import java.util.List;

/**
 * Interface to save and retrieve of {@link UserFeedback} into/from the database.
 */
public interface Feedback {

    /**
     * Save user feedback into repository with the specified {@link UserFeedback} object.
     *
     * @param userFeedback the user feedback object
     */
    void createUserFeedback(UserFeedback userFeedback);

    /**
     * Retrieve user feedback from repository with the specified userName.
     *
     * @param userName User name
     *
     * @return {@link UserFeedback}
     */
    List<UserFeedback> getUserFeedback(UserInfo.Username userName);

    /**
     * Retrieve all user feedbacks from repository.
     *
     * @return List of {@link UserFeedback}s
     */
    List<UserFeedback> getAllUserFeedback();
}
