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
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created on 1/29/16.
 */
public interface Bucket {

    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @GET("experiments/{experimentId}/buckets")
    Response getExperimentBuckets(@Header("Authorization") String authHeader,
                                  @Path("experimentId") String experimentId);


    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @POST("experiments/{experimentId}/buckets")
    Response createBucketForExperiment(@Header("Authorization") String authHeader,
                                       @Path("experimentId") String experimentId,
                                       @Body String newBucketEntity);


    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @PUT("experiments/{experimentId}/buckets")
    Response updateBucketsForExperiment(@Header("Authorization") String authHeader,
                                        @Path("experimentId") String experimentId,
                                        @Body String bucketList);


    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @GET("experiments/{experimentId}/buckets/{bucketLabel}")
    Response getBucketForExperiment(@Header("Authorization") String authHeader,
                                    @Path("experimentId") String experimentId,
                                    @Path("bucketLabel") String bucketLabel);


    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @PUT("experiments/{experimentId}/buckets/{bucketLabel}")
    Response updateBucketForExperiment(@Header("Authorization") String authHeader,
                                       @Path("experimentId") String experimentId,
                                       @Path("bucketLabel") String bucketLabel,
                                       @Body String bucketEntity);

    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @PUT("experiments/{experimentId}/buckets/{bucketLabel}/state/{desiredState}")
    Response updateBucketStateForExperiment(@Header("Authorization") String authHeader,
                                            @Path("experimentId") String experimentId,
                                            @Path("bucketLabel") String bucketLabel,
                                            @Path("desiredState") String desiredState);


    @Headers({"Accept: application/json",
            "User-Agent: TestAgent"})
    @DELETE("experiments/{experimentId}/buckets/{bucketLabel}")
    Response deleteBucketFromExperiment(@Header("Authorization") String authHeader,
                                        @Path("experimentId") String experimentId,
                                        @Path("bucketLabel") String bucketLabel);


}
