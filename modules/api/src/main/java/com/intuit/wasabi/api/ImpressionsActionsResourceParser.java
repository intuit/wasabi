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
package com.intuit.wasabi.api;

import com.intuit.wasabi.experimentobjects.Application;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

class ImpressionsActionsResourceParser {

    private ImpressionsActionsResourceParser() {
    }

    /**
     * Parses JSON submission for impressions and actions.
     * <p>
     * Replaces a null timestamp with the current UTC time.
     *
     * @param body the JSON submission for either impressions or actions
     */
    static void parse(Map<Application.Name, List<List<String>>> body) {
        Date now = new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXX");
        String timestamp = df.format(now);

        for (List<List<String>> submissions : body.values()) {
            for (List<String> submission : submissions) {
                if (submission.get(0) == null) {
                    submission.set(0, timestamp);
                }
            }
        }
    }
}
