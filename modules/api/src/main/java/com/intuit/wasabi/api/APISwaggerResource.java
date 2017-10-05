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

/**
 * Contains all default and example values for Swagger resources
 */
interface APISwaggerResource {

    String DEFAULT_UUID = "54ae563f-242e-43bc-9eeb-0a9454c97110";
    String DEFAULT_DRAFT = "6022afe5-3dd1-427a-b049-c4e4c6e2e229";
    String DEFAULT_BUCKET = "bucketa";
    String DEFAULT_EXP = "swaggerExp";
    String DEFAULT_APP = "swaggerApp";
    String DEFAULT_USERID = "johnDoe";
    String DEFAULT_EVENT = "{\"events\":[{\"name\":\"IMPRESSION\"}]}";
    String DEFAULT_MODEXP = "{\"id\": \"" + DEFAULT_DRAFT + "\",\"label\":\"" + DEFAULT_EXP + "\"," +
            "\"applicationName\":\"" + DEFAULT_EXP + "\",\"description\":\"try to change\"}";
    String DEFAULT_MODBUCK = "{\"label\": \"bucketa\"," +
            "\"allocationPercent\": 0.2," +
            "\"description\": \"bucket for swagger test\",\"isControl\": false}";
    String DEFAULT_PUTBUCK = "{\"label\": \"bucketa\"," +
            "\"allocationPercent\": 0.2," +
            "\"description\": \"bucket modification\",\"isControl\": false}";
    String DEFAULT_BATCHAPP = "example";
    String DEFAULT_LABELLIST = "{\"labels\":[\"expone\", \"exptwo\"]}";
    String DEFAULT_ASSIGNMENT = "{\"assignment\": \"" + DEFAULT_BUCKET + "\"}";
    String DEFAULT_EMPTY = "{}";
    String DEFAULT_ROLE = "admin";

    String EXAMPLE_AUTHORIZATION_HEADER = "Basic d2FzYWJpLW5vcmVwbHlAaW50dWl0LmNvbTp3ZWFrcGFzcw";
    String EXAMPLE_ALL_ROLES = "Example: SUPERADMIN / ADMIN / READONLY / READWRITE";

    String DEFAULT_PAGE = "1";
    String DEFAULT_PER_PAGE = "10";
    String DEFAULT_PER_PAGE_CARDVIEW = "8";
    String DEFAULT_FILTER = "";
    String DEFAULT_SORT = "";
    String DEFAULT_TIMEZONE = "+0000";
    String DEFAULT_ALL = "false";

    String DOC_PAGE = "A positive integer determining the page to return. If the page does not exist, it is " +
            "returned empty.";
    String DOC_PER_PAGE = "A positive integer determining the number of entries per page to return. If -1, all " +
            "entries are returned and the page option is ignored.";
    String DOC_FILTER = "A filter string to filter the elements. The exact semantics depend on the resource, but in " +
            "general the filter follows the pattern: fulltext,key=value,key2=value2,... . It is possible to either " +
            "only supply the fulltext or only key=value pairs. Note that the fulltext must not contain '=' and the " +
            "filter keys and values must not contain '=' or ','. An example would be: matchThis,experiment=myExp";
    String DOC_SORT = "A sort string to sort the elements. The exact semantics depend on the resource, but in " +
            "general the sort order follows the pattern: [-]primary[,[-]secondary[...]] . The keys determine the " +
            "fields and a prefixed hyphen changes the sort order to descending.";
    String DOC_TIMEZONE = "The user's timezone offset. Valid values are for example: +0100, -07:30 or the like.";
    String DOC_All = "A boolean value indicating whether all the underlying configurations should be retrieved.";
}
