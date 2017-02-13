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
package com.intuit.wasabi.assignment;

public class AssignmentsAnnotations {

    //Assignments metadata cache
    public static final String ASSIGNMENTS_METADATA_CACHE_ENABLED = "AssignmentsMetadataCacheEnabled";
    public static final String ASSIGNMENTS_METADATA_CACHE_ALLOWED_STALE_TIME =
            "AssignmentsMetadataCacheAllowedStaleTime";
    public static final String ASSIGNMENTS_METADATA_CACHE_HEALTH_CHECK = "AssignmentsMetadataCacheHealthCheck";
    public static final String ASSIGNMENTS_METADATA_CACHE_REFRESH_CACHE_SERVICE =
            "AssignmentsMetadataCacheRefreshCacheService";
    public static final String ASSIGNMENTS_METADATA_CACHE_REFRESH_INTERVAL = "AssignmentsMetadataCacheRefreshInterval";
    public static final String ASSIGNMENTS_METADATA_CACHE_REFRESH_TASK = "AssignmentsMetadataCacheRefreshTask";

    public static final String ASSIGNMENT_DECORATOR_SERVICE = "assignment.decorator.service";

    public static final String RULECACHE_THREADPOOL = "ruleCache.threadPool";
}
