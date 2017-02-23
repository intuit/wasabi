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
package com.intuit.wasabi.assignment;

public interface AssignmentsAnnotations {

    //Assignments metadata cache
    String ASSIGNMENTS_METADATA_CACHE_ENABLED = "AssignmentsMetadataCacheEnabled";
    String ASSIGNMENTS_METADATA_CACHE_ALLOWED_STALE_TIME = "AssignmentsMetadataCacheAllowedStaleTime";
    String ASSIGNMENTS_METADATA_CACHE_HEALTH_CHECK = "AssignmentsMetadataCacheHealthCheck";
    String ASSIGNMENTS_METADATA_CACHE_REFRESH_CACHE_SERVICE = "AssignmentsMetadataCacheRefreshCacheService";
    String ASSIGNMENTS_METADATA_CACHE_REFRESH_INTERVAL = "AssignmentsMetadataCacheRefreshInterval";
    String ASSIGNMENTS_METADATA_CACHE_REFRESH_TASK = "AssignmentsMetadataCacheRefreshTask";

    String ASSIGNMENT_DECORATOR_SERVICE = "assignment.decorator.service";

    String RULECACHE_THREADPOOL = "ruleCache.threadPool";
}
