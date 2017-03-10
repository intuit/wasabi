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
package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.UserFeedback;

import java.util.Date;

/**
 * Accessor for feedback data
 */
@Accessor
public interface UserFeedbackAccessor {

    /**
     * Create user feedback object
     *
     * @param userId    user id
     * @param submitted date submitted
     * @param score     score in the feedback
     * @param comments  user comments
     * @param contactOk is it ok to contact
     * @param userEmail usr email
     */
    @Query("INSERT INTO user_feedback " +
            "(user_id, submitted, score, comments, contact_okay, user_email) " +
            "VALUES (?, ?, ?, ?, ?, ?)")
    void createUserFeedback(String userId, Date submitted, int score, String comments, boolean contactOk,
                            String userEmail);

    /**
     * Get user feedback for user
     *
     * @param userId user id
     * @return user feedback list
     */
    @Query("select * from user_feedback where user_id = ?")
    Result<UserFeedback> getUserFeedback(String userId);

    /**
     * Get all feedbacks
     *
     * @return list of feedbacks
     */
    @Query("select * from user_feedback")
    Result<UserFeedback> getAllUserFeedback();
}
