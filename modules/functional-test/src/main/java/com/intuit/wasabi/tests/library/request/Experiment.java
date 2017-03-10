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
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

/**
 * Created on 1/29/16.
 */
public interface Experiment {
    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @POST("experiments")
    Response createExperiment(@Header("Authorization") String authHeader,
                              @Query("createNewApplication") String create,
                              @Body String body);


    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @POST("experiments/{experimentId}")
    Response getExperiment(@Header("Authorization") String authHeader,
                           @Path("experimentId") String experimentId);


    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @PUT("experiments/{experimentId}")
    Response updateExperiment(@Header("Authorization") String authHeader,
                              @Path("experimentId") String experimentId,
                              @Query("createNewApplication") String create,
                              @Body String body);


    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @DELETE("experiments/{experimentId}")
    Response deleteExperiment(@Header("Authorization") String authHeader,
                              @Path("experimentId") String experimentId);


}
