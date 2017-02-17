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

import com.google.gson.GsonBuilder;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;

/**
 * A very simple UserRole wrapper.
 */
public class UserRole extends ModelItem {

    /** the name of the application for which we need to give access to the user */
    public String applicationName;

    /** the role of the user against the application. See {@link Constants} for possible roles. */
    public String role;

    /** the userID of the user to whom we want to assign role */
    public String userID;

    /** the email of the user */
    public String userEmail;

    /** the first name of the user */
    public String firstName;

    /** the last name of the user */
    public String lasttName;

    /** The serialization strategy for comparisons and JSON serialization. */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();

    /**
     * Default Constructor
     * Creates an empty UserRole
     */
    public UserRole() {

    }

    /**
     * Creates an userRole.
     * @param applicationName the name of the application
     * @param role the role of the user against the application
     * @param userID the userID of the user 
     */
    public UserRole(String applicationName, String role, String userID) {
        this(applicationName, role, userID, null, null, null);
    }

    /**
     * Cretes an userRole with all the parameters set
     * @param applicationName the name of the application
     * @param role the role of the user against the application
     * @param userID the userID of the user
     * @param userEmail the emailID of the user
     * @param firstName the firstName of the user
     * @param lastName the lastName of the user
     */
    public UserRole(String applicationName, String role, String userID, String userEmail, String firstName, String lastName) {
        this.applicationName = applicationName;
        this.role = role;
        this.userID = userID;
        this.userEmail = userEmail;
        this.firstName = firstName;
        this.lasttName = lastName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLasttName() {
        return lasttName;
    }

    public void setLasttName(String lasttName) {
        this.lasttName = lasttName;
    }


    @Override
    public SerializationStrategy getSerializationStrategy() {
        return UserRole.serializationStrategy;
    }

    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        UserRole.serializationStrategy = serializationStrategy;

    }

    /**
     * Creates an UserRole object from a JSON String.
     * @param json the JSON String.
     * @return an UserRole represented by the JSON String.
     */
    public static UserRole createFromJSONString(String json) {
        return new GsonBuilder().create().fromJson(json, UserRole.class);
    }

}
