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
package com.intuit.wasabi.tests.library.request;

import retrofit.client.Response;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;

/**
 * Created on 1/29/16.
 */

public interface Authentication {
    @Headers({"Cache-Control: no-cache",
            "Accept: application/json",
            "User-Agent: TestAgent"})
    @POST("/authentication/Login")
    Response Login(@Header("Authorization") String authHeader);

    @Headers({"Cache-Control: no-cache",
            "Accept: application/json",
            "User-Agent: TestAgent"})
    @GET("/authentication/Logout")
    Response Logout(@Header("Authorization") String token);

    @Headers({"Cache-Control: no-cache",
            "Accept: application/json",
            "User-Agent: TestAgent"})
    @GET("/authentication/verifyToken")
    Response verifyToken(@Header("Authorization") String token);
}
