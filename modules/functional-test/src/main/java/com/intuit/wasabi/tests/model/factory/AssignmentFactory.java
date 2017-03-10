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
import com.intuit.wasabi.tests.model.Assignment;

/**
 * A factory for Assignments.
 */
public class AssignmentFactory {

    /**
     * Creates a basic Assignment with the required default values.
     *
     * @return a default Assignment.
     */
    public static Assignment createAssignment() {
        return new Assignment();
    }

    /**
     * Creates an Assignment from a JSON String.
     *
     * @param json the JSON String.
     * @return an Assignment represented by the JSON String.
     */
    public static Assignment createFromJSONString(String json) {
        return new GsonBuilder().create().fromJson(json, Assignment.class);
    }
}
