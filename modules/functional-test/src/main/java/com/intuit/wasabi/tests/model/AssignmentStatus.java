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
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;

/**
 * A very simple AssignmentStatus wrapper..
 */
public class AssignmentStatus extends ModelItem {

    /** the status of the role assignment */
    private String roleAssignmentStatus;

    /** the role of the user */
    private String role;

    /** the userID of the user */
    private String userID;

    /** the name  of the application */
    private String applicationName;

    /** The serialization strategy for comparisons and JSON serialization. */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();


    /**
     * Default Constructor
     */
    public AssignmentStatus() {

    }

    /**
     * Constructor that initializes 
     * AssignmentStatus with values specified 
     * @param roleAssignmentStatus the status of role assignment
     * @param role the name of the role
     * @param userID the userID of the user 
     * @param applicationName the name of the application
     */
    public AssignmentStatus(String roleAssignmentStatus, String role, String userID, String applicationName) {
        this.roleAssignmentStatus = roleAssignmentStatus;
        this.role = role;
        this.userID = userID;
        this.applicationName = applicationName;
    }

    public String getRoleAssignmentStatus() {
        return roleAssignmentStatus;
    }

    public void setRoleAssignmentStatus(String roleAssignmentStatus) {
        this.roleAssignmentStatus = roleAssignmentStatus;
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

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    @Override
    public SerializationStrategy getSerializationStrategy() {
        return serializationStrategy;
    }

    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        AssignmentStatus.serializationStrategy = serializationStrategy;
    }

    /**
     * Creates an AssignmentStatus from a JSON String.
     * @param json the JSON String.
     * @return an AssignmentStatus represented by the JSON String.
     */
    public static AssignmentStatus createFromJSONString(String json) {
        return new GsonBuilder().create().fromJson(json, AssignmentStatus.class);
    }

}
