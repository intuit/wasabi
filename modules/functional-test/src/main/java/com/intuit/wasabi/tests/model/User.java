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
public class User extends ModelItem {
    /**
     * The user name
     */
    public String userID;

    /**
     * The serialization strategy for comparisons and JSON serialization.
     */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();

    /**
     * Copies a user.
     *
     * @param other the user to copy.
     */
    public User(User other) {
        update(other);
    }

    /**
     * Creates a user with a specific ID.
     *
     * @param userID the userID.
     */
    public User(String userID) {
        this.userID = userID;
    }

    /**
     * Sets the userID and returns this instance.
     *
     * @param userID the userID
     * @return this
     */
    public User setUserId(String userID) {
        this.userID = userID;
        return this;
    }

    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        User.serializationStrategy = serializationStrategy;
    }

    @Override
    public SerializationStrategy getSerializationStrategy() {
        return User.serializationStrategy;
    }

}
