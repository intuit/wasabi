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
package com.intuit.wasabi.tests.model;

import com.intuit.wasabi.tests.library.util.TestUtils;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;

/**
 * A very simple UserFeedback wrapper.
 */
public class UserFeedback extends ModelItem {

    /** The serialization strategy for comparisons and JSON serialization. */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();
    private String username;
    /**
     * The submitted time. Should be formatted {@code yyyy-MM-dd'T'hh:mm:ssZ}.
     * Use the {@link TestUtils} to create it. Required.
     */
    private String submitted;
    private int score;
    private String comments;
    private boolean contactOkay = false;
    private String email;

    /**
     * Creates an empty experiment.
     * @param username    the user name
     */
    public UserFeedback(String username) {
        this.username = username;
    }

    /**
     * Creates a deep copy of the {@code other} UserFeedback.
     *
     * @param other a UserFeedback to copy.
     */
    public UserFeedback(UserFeedback other) {
        update(other);
    }

    public String getUsername() {
        return username;
    }

    public String getSubmitted() {
        return submitted;
    }

    /**
     * Sets the submitted and returns this instance.
     *
     * @param submitted the submitted user feedback
     * @return the current UserFeedback Object
     */
    public UserFeedback setSubmitted(String submitted) {
        this.submitted = submitted;
        return this;
    }

    public int getScore() {
        return score;
    }

    /**
     * Sets the score and returns this instance.
     *
     * @param score the current score
     * @return the current UserFeedback Object
     */
    public UserFeedback setScore(int score) {
        this.score = score;
        return this;
    }

    public String getComments() {
        return comments;
    }

    /**
     * Sets the comments and returns this instance.
     *
     * @param comments the user feedback comment
     * @return the current UserFeedback Object
     */
    public UserFeedback setComments(String comments) {
        this.comments = comments;
        return this;
    }

    public boolean isContactOkay() {
        return contactOkay;
    }

    /**
     * Sets the contactOkay and returns this instance.
     *
     * @param contactOkay the contact ok consent, true for OK
     * @return the current UserFeedback Object
     */
    public UserFeedback setContactOkay(boolean contactOkay) {
        this.contactOkay = contactOkay;
        return this;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Sets the email and returns this instance.
     *
     * @param email email address
     * @return the current UserFeedback Object
     */
    public UserFeedback setEmail(String email) {
        this.email = email;
        return this;
    }

    @Override
    public SerializationStrategy getSerializationStrategy() {
        return UserFeedback.serializationStrategy;
    }

    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        UserFeedback.serializationStrategy = serializationStrategy;
    }

}
