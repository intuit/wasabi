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

package com.intuit.wasabi.assignment.impl;

import com.codahale.metrics.health.HealthCheck;
import com.intuit.wasabi.assignment.cache.AssignmentMetadataCacheTimeService;
import com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache;
import com.intuit.wasabi.assignment.cache.impl.AssignmentsMetadataCacheHealthCheck;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Class to unit test Health check for assignment metadata cache.
 */
public class AssignmentsMetadataCacheHealthCheckTest {

    //Mock dependant services/objects
    AssignmentsMetadataCache metadataCache = mock(AssignmentsMetadataCache.class);
    AssignmentMetadataCacheTimeService timeService = mock(AssignmentMetadataCacheTimeService.class);
    Integer metadataCacheInterval = 5;
    Integer allowedStaleTime = 30;
    Boolean metadataCacheEnabled = Boolean.TRUE;


    @Test
    public void happyPathTestCacheDisabled() {
        //Call actual business logic
        AssignmentsMetadataCacheHealthCheck healthCheck = new AssignmentsMetadataCacheHealthCheck(metadataCache, timeService, metadataCacheInterval, allowedStaleTime, Boolean.FALSE);
        HealthCheck.Result result = healthCheck.check();

        //Verify result - It should be Healthy as cache is disabled
        assertThat(result.isHealthy(), is(true));

    }

    @Test
    public void happyPathTestCacheEnabled() {
        Calendar currentTime = GregorianCalendar.getInstance();
        currentTime.set(2017, 02, 05, 03, 30);
        when(timeService.getCurrentTime()).thenReturn(currentTime.getTime());

        Calendar currentTimeMinus2Minutes = GregorianCalendar.getInstance();
        currentTimeMinus2Minutes.set(2017, 02, 05, 03, 28);
        when(metadataCache.getLastRefreshTime()).thenReturn(currentTimeMinus2Minutes.getTime());

        //Call actual business logic
        AssignmentsMetadataCacheHealthCheck healthCheck = new AssignmentsMetadataCacheHealthCheck(metadataCache, timeService, metadataCacheInterval, allowedStaleTime, metadataCacheEnabled);
        HealthCheck.Result result = healthCheck.check();

        //Verify result - It should be healthy as cache was refreshed (current_time - 2 minutes)
        assertThat(result.isHealthy(), is(true));

    }

    @Test
    public void errorPathTestCacheNotRefreshedInLast2Runs() {
        Calendar currentTime = GregorianCalendar.getInstance();
        currentTime.set(2017, 02, 05, 03, 30);
        when(timeService.getCurrentTime()).thenReturn(currentTime.getTime());

        Calendar currentTimeMinus11Minutes = GregorianCalendar.getInstance();
        currentTimeMinus11Minutes.set(2017, 02, 05, 03, 19);
        when(metadataCache.getLastRefreshTime()).thenReturn(currentTimeMinus11Minutes.getTime());

        //Call actual business logic
        AssignmentsMetadataCacheHealthCheck healthCheck = new AssignmentsMetadataCacheHealthCheck(metadataCache, timeService, metadataCacheInterval, allowedStaleTime, metadataCacheEnabled);
        HealthCheck.Result result = healthCheck.check();

        //Verify result - It should be healthy as cache was NOT refreshed in last interval window but it is still within allowed stale window.
        assertThat(result.isHealthy(), is(true));
    }

    @Test
    public void errorPathTestCacheNotRefreshedSinceAllowedStaleTime() {
        Calendar currentTime = GregorianCalendar.getInstance();
        currentTime.set(2017, 02, 05, 03, 30);
        when(timeService.getCurrentTime()).thenReturn(currentTime.getTime());

        Calendar currentTimeMinus31Minutes = GregorianCalendar.getInstance();
        currentTimeMinus31Minutes.set(2017, 02, 05, 02, 59);
        when(metadataCache.getLastRefreshTime()).thenReturn(currentTimeMinus31Minutes.getTime());

        //Call actual business logic
        AssignmentsMetadataCacheHealthCheck healthCheck = new AssignmentsMetadataCacheHealthCheck(metadataCache, timeService, metadataCacheInterval, allowedStaleTime, metadataCacheEnabled);
        HealthCheck.Result result = healthCheck.check();

        //Verify result - It should NOT be healthy as cache was NOT refreshed since allowed stale time
        assertThat(result.isHealthy(), is(false));
    }

    @Test
    public void errorPathTestExceptionHappenedWhileGettingLastRefreshTime() {
        Calendar currentTime = GregorianCalendar.getInstance();
        currentTime.set(2017, 02, 05, 03, 30);
        when(timeService.getCurrentTime()).thenReturn(currentTime.getTime());

        Calendar currentTimeMinus2Minutes = GregorianCalendar.getInstance();
        currentTimeMinus2Minutes.set(2017, 02, 05, 30, 28);
        when(metadataCache.getLastRefreshTime()).thenThrow(new NullPointerException());

        //Call actual business logic
        AssignmentsMetadataCacheHealthCheck healthCheck = new AssignmentsMetadataCacheHealthCheck(metadataCache, timeService, metadataCacheInterval, allowedStaleTime, metadataCacheEnabled);
        HealthCheck.Result result = healthCheck.check();

        //Verify result - It should NOT be healthy as some exception happened while checking Health
        assertThat(result.isHealthy(), is(false));
    }
}
