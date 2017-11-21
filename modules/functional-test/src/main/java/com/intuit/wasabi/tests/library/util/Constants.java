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
package com.intuit.wasabi.tests.library.util;


import org.apache.commons.codec.binary.Base64;

public class Constants {

    public static final String USER_USER_ID = "userID";
    public static final String WASABI_EMAIL = "wasabi_reader@example.com";
    public static final String WASABI_LAST_NAME = "Reader";
    public static final String WASABI_FIRST_NAME = "Wasabi";
    public static final String USER_EMAIL = "userEmail";
    public static final String USER_ROLE = "role";
    public static final String USER_LAST_NAME = "lastName";
    public static final String USER_FIRST_NAME = "firstName";
    public static final String USER_READER = "wasabi_reader";

    public static final String DEFAULT_CONFIG_FILE = "config.properties";
    public static final String DEFAULT_CONFIG_SERVER_PROTOCOL = "http";
    public static final String DEFAULT_CONFIG_SERVER_NAME = "localhost:8080";
    public static final String DEFAULT_CONFIG_API_VERSION_STRING = "v1";
    public static final String DEFAULT_PAGE_NAME = "homepage";
    public static final String DEFAULT_NODE_COUNT = String.valueOf(1);
    public static final String DEFAULT_TEST_USER = "wasabi_reader@example.com";
        
    public static final String NEW_LINE = System.getProperty("line.separator"); // OS dependent line separator
    public static final String TAB = "\t";

    public static final int DEFAULT_RAPIDEXP_MAX_USERS  = 10;
    
    /**
     * The integration tests prefix, should be {@code SW50ZWdyVGVzdA_}
     */
    public static final String INTEGRATION_TESTS_PREFIX = new String(Base64.encodeBase64("IntegrTest".getBytes())).replace("==", "_");

    /**
     * {@link #INTEGRATION_TESTS_PREFIX}{@code + "Application_";}
     */
    public static final String DEFAULT_PREFIX_APPLICATION = INTEGRATION_TESTS_PREFIX + System.currentTimeMillis() + "App_";

    public static final String NEW_PREFIX_APPLICATION = INTEGRATION_TESTS_PREFIX + System.currentTimeMillis() + "App_New_";

    /**
     * {@link #INTEGRATION_TESTS_PREFIX}{@code + "Bucket_";}
     */
    public static final String DEFAULT_PREFIX_BUCKET = INTEGRATION_TESTS_PREFIX + "Bucket_";

    /**
     * {@link #INTEGRATION_TESTS_PREFIX}{@code + "Experiment_";}
     */
    public static final String DEFAULT_PREFIX_EXPERIMENT = INTEGRATION_TESTS_PREFIX + "Experiment_";
    
    /**
     * {@link #INTEGRATION_TESTS_PREFIX}{@code + "Tag_";}
     */
    public static final String DEFAULT_PREFIX_TAG = INTEGRATION_TESTS_PREFIX + "Tag_";

    /**
     * {@link #INTEGRATION_TESTS_PREFIX}{@code + "User_";}
     */
    public static final String DEFAULT_PREFIX_USER = INTEGRATION_TESTS_PREFIX + "User_";

    /**
     * {@link #INTEGRATION_TESTS_PREFIX}{@code + "Page_";}
     */
    public static final String DEFAULT_PREFIX_PAGE = INTEGRATION_TESTS_PREFIX + "Page_";

    /**
     * {@link #INTEGRATION_TESTS_PREFIX}{@code + "UserFeedback_";}
     */
    public static final String DEFAULT_PREFIX_USER_FEEDBACK = INTEGRATION_TESTS_PREFIX + "UserFeedback_";

    public static final String EXPERIMENT_STATE_DRAFT = "DRAFT";
    public static final String EXPERIMENT_STATE_RUNNING = "RUNNING";
    public static final String EXPERIMENT_STATE_PAUSED = "PAUSED";
    public static final String EXPERIMENT_STATE_TERMINATED = "TERMINATED";
    public static final String EXPERIMENT_STATE_DELETED = "DELETED";

    public static final String ROLE_READONLY = "READONLY";
    public static final String ROLE_READWRITE = "READWRITE";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SUPERADMIN = "SUPERADMIN";

    public static final String BUCKET_STATE_OPEN = "OPEN";
    public static final String BUCKET_STATE_CLOSED = "CLOSED";
    public static final String BUCKET_STATE_EMPTY = "EMPTY";

    public static final String ASSIGNMENT_NO_OPEN_BUCKETS = "NO_OPEN_BUCKETS";
    public static final String ASSIGNMENT_NO_PROFILE_MATCH = "NO_PROFILE_MATCH";
    public static final String ASSIGNMENT_NEW_ASSIGNMENT = "NEW_ASSIGNMENT";
    public static final String ASSIGNMENT_EXISTING_ASSIGNMENT = "EXISTING_ASSIGNMENT";
    public static final String ASSIGNMENT_EXPERIMENT_IN_DRAFT_STATE = "EXPERIMENT_IN_DRAFT_STATE";

    public static final String NEW_ASSIGNMENT = "NEW_ASSIGNMENT";
    public static final String NO_OPEN_BUCKETS = "NO_OPEN_BUCKETS";

    public static final int EXP_SPAWN_COUNT = 5;

    public static final String ROLE_LIST = "roleList";

}
