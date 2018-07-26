/*******************************************************************************
 * Copyright 2017 Intuit
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

public interface ApiAnnotations {
    String APPLICATION_ID = "application.id";
    String DEFAULT_TIME_ZONE = "default.time.zone";
    String DEFAULT_TIME_FORMAT = "default.time.format";
    String ACCESS_CONTROL_MAX_AGE_DELTA_SECONDS = "access.control.max.age.delta.seconds";

    String RATE_HOURLY_LIMIT="rate.hourly.limit";
    String RATE_LIMIT_ENABLED="rate.limit.enabled";
}
