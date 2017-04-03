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
import com.intuit.wasabi.tests.model.Page;

/**
 * A factory for Pages.
 */
public class PageFactory {

    /**
     * Only used to create unique Page labels.
     */
    private static int internalId = 0;

    /**
     * Creates a basic Page with the required default values.
     *
     * @return a default Page.
     */
    public static Page createPage() {
        return new Page(Constants.DEFAULT_PREFIX_PAGE + internalId++, true);
    }

    /**
     * Creates an Page from a JSON String.
     *
     * @param json the JSON String.
     * @return an Page represented by the JSON String.
     */
    public static Page createFromJSONString(String json) {
        return new GsonBuilder().create().fromJson(json, Page.class);
    }
}
