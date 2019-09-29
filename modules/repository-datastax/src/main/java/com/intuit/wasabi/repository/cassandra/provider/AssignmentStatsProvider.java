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
package com.intuit.wasabi.repository.cassandra.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.repository.cassandra.accessor.count.HourlyBucketCountAccessor;
import com.intuit.wasabi.repository.cassandra.impl.AssignmentStats;

public class AssignmentStatsProvider implements Provider<AssignmentStats> {

    private HourlyBucketCountAccessor hourlyBucketCountAccessor;

    @Inject
    public AssignmentStatsProvider(HourlyBucketCountAccessor hourlyBucketCountAccessor){
        this.hourlyBucketCountAccessor = hourlyBucketCountAccessor;
    }

    @Override
    public AssignmentStats get(){
        return new AssignmentStats(hourlyBucketCountAccessor);
    }
}
