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
package com.intuit.wasabi.tests.library.request;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;

public interface Event {

    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @GET("experiments/{experimentId}/events")
    Response getExperimentEventss(@Header("Authorization") String authHeader,
                                  @Path("experimentId") String experimentId);


    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @POST("experiments/{experimentId}/events")
    Response createEventsForExperiment(@Header("Authorization") String authHeader,
                                       @Path("experimentId") String experimentId,
                                       @Body String eventParameters);


}
