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

import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;

/**
 * A very simple User wrapper.
 */
public class APIUser extends ModelItem {

    /**
     * The user name. required field
     */
    public String username;

    /**
     * user id
     */
    public String userId;

    /**
     * first name
     */
    public String firstName;

    /**
     * last name
     */
    public String lastName;

    /**
     * email
     */
    public String email;

    /**
     * password
     */
    public String password;

    /**
     * The serialization strategy for comparisons and JSON serialization.
     */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();

    /**
     * Copies a user.
     *
     * @param other the user to copy.
     */
    public APIUser(APIUser other) {
        update(other);
    }

    /**
     * Creates a user with a specific ID.
     *
     * @param username the userID.
     */
    public APIUser(String username) {
        this.username = username;
    }

    /**
     * Sets the username and returns this instance.
     *
     * @param username the username
     * @return this
     */
    public APIUser setUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * Sets the userId and returns this instance.
     *
     * @param userId the userId
     * @return this
     */
    public APIUser setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * Sets the firstName and returns this instance.
     *
     * @param firstName the first name
     * @return this
     */
    public APIUser setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    /**
     * Sets the lastName and returns this instance.
     *
     * @param lastName the last name
     * @return this
     */
    public APIUser setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    /**
     * Sets the email and returns this instance.
     *
     * @param email the email
     * @return this
     */
    public APIUser setEmail(String email) {
        this.email = email;
        return this;
    }

    /**
     * Sets the password and returns this instance.
     *
     * @param password the password
     * @return this
     */
    public APIUser setPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        APIUser.serializationStrategy = serializationStrategy;
    }

    @Override
    public SerializationStrategy getSerializationStrategy() {
        return APIUser.serializationStrategy;
    }

}
