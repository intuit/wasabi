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
package com.intuit.wasabi.tests.model.factory;

import com.google.gson.GsonBuilder;
import com.intuit.wasabi.tests.library.util.Constants;
import com.intuit.wasabi.tests.model.UserFeedback;

/**
 * A factory for UserFeedback.
 */
public class UserFeedbackFactory {

    /**
     * Only used to create unique UserFeedback.
     */
    private static int internalId = 0;

    /**
     * Creates a user feedback with the required default values.
     *
     * @return a default Page.
     */
    public static UserFeedback createUserFeedback() {
        UserFeedback userFeedback = new UserFeedback(Constants.DEFAULT_PREFIX_USER_FEEDBACK + internalId++);
        return userFeedback;
    }

    /**
     * Creates a UserFeedback from a JSON String.
     *
     * @param json the JSON String.
     * @return an UserFeedback represented by the JSON String.
     */
    public static UserFeedback createFromJSONString(String json) {
        return new GsonBuilder().create().fromJson(json, UserFeedback.class);
    }
}
