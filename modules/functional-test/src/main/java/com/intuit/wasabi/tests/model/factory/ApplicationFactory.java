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
import com.intuit.wasabi.tests.model.Application;

import java.util.ArrayList;
import java.util.List;

/**
 * A factory for Applications.
 */
public class ApplicationFactory {

    /**
     * Only used to create unique Application labels.
     */
    private static int internalId = 0;

    /**
     * the primary application to keep it consistent across the board.
     */
    private static final Application defaultApplication = new Application(Constants.DEFAULT_PREFIX_APPLICATION + "PRIMARY");

    /**
     * Returns the default application.
     *
     * @return the default application
     */
    public static Application defaultApplication() {
        return defaultApplication;
    }

    /**
     * Creates a basic Application with the required default values.
     *
     * @return a default Application.
     */
    public static Application createApplication() {
        return new Application(Constants.DEFAULT_PREFIX_APPLICATION + String.valueOf(internalId++));
    }

    /**
     * Creates a List of Applications with the required names specified in the list.
     *
     * @param applicationNames - the list of the names of the application one want
     * @return a list Applications with the specified names.
     */
    public static List<Application> createApplications(List<String> applicationNames) {
        List<Application> applicationsList = new ArrayList<Application>();
        for (int i = 0; i < applicationNames.size(); i++) {
            Application application = new Application(applicationNames.get(i));
            applicationsList.add(application);
        }
        return applicationsList;
    }

    /**
     * Creates an Application from a JSON String.
     *
     * @param json the JSON String.
     * @return an Application represented by the JSON String.
     */
    public static Application createFromJSONString(String json) {
        return new GsonBuilder().create().fromJson(json, Application.class);
    }
}
