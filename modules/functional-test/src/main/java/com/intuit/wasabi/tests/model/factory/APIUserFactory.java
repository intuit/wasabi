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
import com.intuit.wasabi.tests.model.APIUser;

/**
 * A simple APIUser factory.
 */
public class APIUserFactory {
    private static int internalId = 0;

    /**
     * Creates an APIUser with a unique ID.
     *
     * @return an APIUser.
     */
    public static APIUser createAPIUser() {
        return new APIUser("APIUser" + internalId++);
    }

    /**
     * Creates an APIUser from a JSON String.
     *
     * @param json API User Json formatted string
     * @return the APIUser Object {@link APIUser}
     */
    public static APIUser createFromJSONString(String json) {
        return new GsonBuilder().create().fromJson(json, APIUser.class);
    }
}
