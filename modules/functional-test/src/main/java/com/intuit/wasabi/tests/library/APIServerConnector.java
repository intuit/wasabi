/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.tests.library;

import com.google.gson.Gson;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.filter.log.LogDetail;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.specification.RequestSpecification;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.preemptive;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An <tt>APIServerConnector</tt> object stores the current server url, and provides methods for doing REST GET, POST, etc. to the server.
 * <p>
 * If a non-null user name is passed to the constructor, then basic, preemptive authentication will be used.
 * <p>
 * The {@code clone} method is meant for making a clone of a connector that then can be modified using the setters,
 * thus avoiding to have to specify all arguments that otherwise go into a constructor call.
 */

public class APIServerConnector {

    private static final Logger LOGGER = getLogger(APIServerConnector.class);
    private RequestSpecification requestSpec;
    //     The constructor will set up a default request specification to use.
    //     See description at:
    //     http://rest-assured.googlecode.com/svn/tags/2.3.4/apidocs/index.html?com/jayway/restassured/builder/RequestSpecBuilder.html
    private String baseUri;
    private String basePath;
    private String userName;
    private String password;
    private Map<String, String> headerMap;
    private ContentType contentType = ContentType.JSON;

    /**
     * Instantiates an {@code APIServerConnector} object.
     *
     * @param baseUri   The server uri including http or https and possibly with :portnumber
     * @param basePath  the base path
     * @param userName  the user name
     * @param password  the password
     * @param headerMap the headers
     */
    public APIServerConnector(String baseUri, String basePath, String userName, String password, Map<String, String> headerMap) {
        this.baseUri = baseUri;
        this.basePath = basePath;
        this.userName = userName;
        this.password = password;
        this.headerMap = headerMap;
        this.requestSpec = constructRequestSpec();
    }

    /**
     * Instantiates an {@code APIServerConnector} object.
     *
     * @param baseUri  The server uri including http or https and possibly with :portnumber
     * @param basePath the base path
     * @param userName the user name
     * @param password the password
     */
    public APIServerConnector(String baseUri, String basePath, String userName, String password) {
        this.baseUri = baseUri;
        this.basePath = basePath;
        this.userName = userName;
        this.password = password;
        this.headerMap = new HashMap<>();
        this.requestSpec = constructRequestSpec();
    }

    // Setter methods
    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
        this.requestSpec = constructRequestSpec();
    }

    public void setUserNameAndPassword(String userName, String password) {
        this.userName = userName;
        this.password = password;
        this.requestSpec = constructRequestSpec();
    }

    public void setAuthToken(String realm, String token) {
        this.putHeaderMapKVP("Authorization", realm + " " + token);
    }

    public void setContentType(ContentType contentType) {
        this.contentType = contentType;
        this.requestSpec = constructRequestSpec();
    }

    public void setHeaderMap(Map<String, String> headerMap) {
        this.headerMap = headerMap;
        this.requestSpec = constructRequestSpec();
    }

    // Modify methods
    public void putHeaderMapKVP(String key, String value) {
        headerMap.put(key, value);
        this.requestSpec = constructRequestSpec();
    }

    public void removeHeaderMapKey(String key) {
        headerMap.remove(key);
        this.requestSpec = constructRequestSpec();
    }

    // Public utility methods
    public APIServerConnector clone() {
        // NIT Consider replacing this by the Object.clone method it overrides
        APIServerConnector theClone = new APIServerConnector(
                this.baseUri,
                this.basePath,
                this.userName,
                this.password,
                this.headerMap);
        theClone.setContentType(this.contentType);
        return theClone;
    }

    // Private helper Methods
    public String getJsonString(Object jsonBody) {
        String jsonString = null;
        if (Objects.nonNull(jsonBody)) {
            if (jsonBody.getClass() == String.class) {
                jsonString = (String) jsonBody;
            } else {
                Gson gson = new Gson();
                jsonString = gson.toJson(jsonBody);
            }
        }
        return jsonString;
    }


    // Constructor

    /**
     * Construct a {@code curl} command equivalent to the call made from Java given the passed in values
     *
     * @param method   The HTTP method, i.e. "GET", "POST", ...
     * @param url      The full URL used for the call
     * @param formJSON Any body as a JSON string to add to the call, or null
     * @return A String with a curl command corresponding to the arguments
     */
    private String curlCallString(String method, String url, String formJSON) {

        String dataString = "";
        if (Objects.nonNull(formJSON) && !formJSON.isEmpty()) {
            dataString = "-d '" + formJSON + "' ";
        }

        String authString = "";
        if (Objects.nonNull(userName) && !userName.isEmpty()) {
            authString = "-u $api_user:$api_user_password "; // Don't print actual values!
        }

        String agentString = "";
        String contentTypeString = "-H \"Content-Type:application/json\" "; // Default is JSON
        if (Objects.nonNull(this.headerMap)) {
            for (Map.Entry<String, String> entry : this.headerMap.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                switch (key) {
                    case "User-Agent":
                        agentString = "-A " + value + " ";
                        break;
                    case "Content-Type":
                        contentTypeString = "-H \"Content-Type:" + value + "\" ";
                        break;
                    case "Authorization":
                        authString = "-H \"Authorization: " + value + "\"";
                        break;
                    default:
                        throw new IllegalArgumentException("Support for key \"" + key + "\" not implemented yet.");
                }
            }
        }

        return "curl " +
                "-X " + method + " " +
                authString +
                agentString +
                contentTypeString +
                dataString +
                baseUri + basePath + url;
    }

    private RequestSpecification constructRequestSpec() {
        RequestSpecBuilder reqBuilder = new RequestSpecBuilder();
        reqBuilder.setContentType(this.contentType);
        reqBuilder.setBaseUri(this.baseUri);
        reqBuilder.setBasePath(this.basePath);
        reqBuilder.setRelaxedHTTPSValidation();
        reqBuilder.log(LogDetail.ALL); // NIT use setting to control this

        if (Objects.nonNull(userName)) {
            reqBuilder.setAuth(preemptive().basic(this.userName, this.password));
        }
        if (Objects.nonNull(headerMap)) {
            reqBuilder.addHeaders(headerMap);
        }

        return reqBuilder.build();
        // NOTE
        // reqBuilder.setRelaxedHTTPSValidation();
        // was put in because we did not have certificate for the host
        // https://code.google.com/p/rest-assured/wiki/Usage#SSL
    }

    // HTTP method calls

    /**
     * Performs a request using the specified method (POST, PUT, DELETE, GET - defaults to GET on wrong inputs).
     *
     * @param method   Either POST, PUT, DELETE, GET.
     * @param url      the url to call
     * @param jsonBody the content body
     * @return the response
     */
    private Response doRequest(String method, String url, Object jsonBody) {
        String formJSON = getJsonString(jsonBody);
        LOGGER.info(curlCallString(method, url, formJSON));

        RequestSpecification specification = given().spec(this.requestSpec);
        if (Objects.nonNull(formJSON)) {
            specification = specification.body(formJSON);
        }

        Response response;
        long startTime = System.currentTimeMillis();
        switch (method) {
            case "POST":
                response = specification.post(url);
                break;
            case "PUT":
                response = specification.put(url);
                break;
            case "DELETE":
                response = specification.delete(url);
                break;
            case "GET":
            default:
                response = specification.get(url);
        }
        long endTime = System.currentTimeMillis();
        LOGGER.info("Duration for {} {} : {} ms", method, url, endTime - startTime);

        return response;
    }

    /**
     * Does a HTTP POST with the given {@code formJSON} in the body
     *
     * @param url      The API call part of the URL (i.e. everything after ".../api/v1/").
     * @param jsonBody The JSON body to pass to the REST API call. Can be either a String, or an Object which can convert to a JSON string using Gson's toJson method.
     * @return The response object returned
     */

    public Response doPost(String url, Object jsonBody) {
        return doRequest("POST", url, jsonBody);
    }

    /**
     * Does a HTTP POST with an empty body
     *
     * @param url The API call part of the URL (i.e. everything after ".../api/v1/").
     * @return The response object returned
     */
    public Response doPost(String url) {
        return doPost(url, null);
    }

    /**
     * Does a HTTP PUT with the given {@code formJSON} in the body
     *
     * @param url      The API call part of the URL (i.e. everything after ".../api/v1/").
     * @param jsonBody The JSON body to pass to the REST API call. Can be either a String, or an Object which can convert to a JSON string using Gson's toJson method.
     * @return The response object returned
     */
    public Response doPut(String url, Object jsonBody) {
        return doRequest("PUT", url, jsonBody);
    }

    /**
     * Does a HTTP PUT with an empty body
     *
     * @param url The API call part of the URL (i.e. everything after ".../api/v1/").
     * @return The response object returned
     */
    public Response doPut(String url) {
        return doPut(url, null);
    }


    /**
     * Does a HTTP GET
     *
     * @param url      The API call part of the URL (i.e. everything after ".../api/v1/")
     * @param jsonBody The JSON body to pass to the REST API call. Can be either a String, or an Object which can convert to a JSON string using Gson's toJson method.
     * @return The response object returned
     */
    public Response doGet(String url, Object jsonBody) {
        return doRequest("GET", url, jsonBody);
    }

    /**
     * Does a HTTP GET with an empty body
     *
     * @param url The API call part of the URL (i.e. everything after ".../api/v1/").
     * @return The response object returned
     */
    public Response doGet(String url) {
        return doGet(url, null);
    }

    /**
     * Does a HTTP DELETE
     *
     * @param url      The API call part of the URL (i.e. everything after ".../api/v1/")
     * @param jsonBody The JSON body to pass to the REST API call. Can be either a String, or an Object which can convert to a JSON string using Gson's toJson method.
     * @return The response object returned
     */
    public Response doDelete(String url, Object jsonBody) {
        return doRequest("DELETE", url, jsonBody);
    }

    /**
     * Does a HTTP DELETE with an empty body
     *
     * @param url The API call part of the URL (i.e. everything after ".../api/v1/").
     * @return The response object returned
     */
    public Response doDelete(String url) {
        return doDelete(url, null);
    }

}
