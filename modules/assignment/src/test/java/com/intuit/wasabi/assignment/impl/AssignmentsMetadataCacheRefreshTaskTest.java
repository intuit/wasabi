/*
  *****************************************************************************
  Copyright 2017 Intuit

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package com.intuit.wasabi.assignment.impl;

import com.intuit.wasabi.assignment.cache.AssignmentMetadataCacheTimeService;
import com.intuit.wasabi.assignment.cache.AssignmentsMetadataCache;
import com.intuit.wasabi.assignment.cache.impl.AssignmentsMetadataCacheRefreshTask;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Date;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Class to unit test AssignmentsMetadataCacheRefreshTask
 */
public class AssignmentsMetadataCacheRefreshTaskTest {

    @Test
    public void refreshCacheHappyPath1() throws InterruptedException {
        //Create mocks
        AssignmentsMetadataCache metadataCache = mock(AssignmentsMetadataCache.class);
        AssignmentMetadataCacheTimeService timeService = mock(AssignmentMetadataCacheTimeService.class);
        Date cTime = new Date();
        when(timeService.getCurrentTime()).thenReturn(cTime);
        when(metadataCache.refresh()).thenReturn(Boolean.TRUE);

        AssignmentsMetadataCacheRefreshTask task = new AssignmentsMetadataCacheRefreshTask(metadataCache, timeService);

        //When task is created, isRefreshInProgress should be FALSE
        assertThat(task.isRefreshInProgress(), is(false));

        //Actual invocation of AssignmentsMetadataCacheRefreshTask
        Thread t1 = new Thread(task);
        t1.start();
        t1.join();

        //Verify that refresh should be called only once
        verify(metadataCache, times(1)).refresh();

        //Verify that clear should NOT be called - as there was no exception
        verify(metadataCache, times(0)).clear();

        //Last refresh time should be the provided current time.
        assertThat(cTime, is(task.getLastRefreshTime()));

        //After task completion, RefreshInProgress flag should be FALSE
        assertThat(task.isRefreshInProgress(), is(false));

    }

    @Test
    public void refreshCacheErrorPath1() throws InterruptedException {
        //Create mocks
        AssignmentsMetadataCache metadataCache = mock(AssignmentsMetadataCache.class);
        AssignmentMetadataCacheTimeService timeService = mock(AssignmentMetadataCacheTimeService.class);
        Date cTime = new Date();
        when(timeService.getCurrentTime()).thenReturn(cTime);
        //Throw exception when refresh is called
        when(metadataCache.refresh()).thenThrow(new NullPointerException());

        AssignmentsMetadataCacheRefreshTask task = new AssignmentsMetadataCacheRefreshTask(metadataCache, timeService);

        //When task is created, isRefreshInProgress should be FALSE
        assertThat(task.isRefreshInProgress(), is(false));

        //Actual invocation of AssignmentsMetadataCacheRefreshTask
        Thread t1 = new Thread(task);
        t1.start();
        t1.join();

        //Verify that refresh should be called only once
        verify(metadataCache, times(1)).refresh();

        //Verify that clear should NOT be called - as there was no exception
        verify(metadataCache, times(1)).clear();

        //Last refresh time should be the provided current time.
        assertThat(cTime, is(task.getLastRefreshTime()));

        //After task completion, RefreshInProgress flag should be FALSE
        assertThat(task.isRefreshInProgress(), is(false));

    }

    @Test
    public void refreshCacheUpdateOfLastRefreshTime() throws InterruptedException {
        //Create mocks
        AssignmentsMetadataCache metadataCache = mock(AssignmentsMetadataCache.class);
        AssignmentMetadataCacheTimeService timeService = mock(AssignmentMetadataCacheTimeService.class);
        Date cTime1 = new Date();
        when(timeService.getCurrentTime()).thenReturn(cTime1);
        when(metadataCache.refresh()).thenReturn(Boolean.TRUE);

        AssignmentsMetadataCacheRefreshTask task = new AssignmentsMetadataCacheRefreshTask(metadataCache, timeService);
        //When task is created, isRefreshInProgress should be FALSE
        assertThat(task.isRefreshInProgress(), is(false));
        //When task is created, Last refresh time should be the provided cTime1.
        assertThat(cTime1, is(task.getLastRefreshTime()));

        //Change current time
        Date cTime2 = new Date();
        when(timeService.getCurrentTime()).thenReturn(cTime2);

        //Actual invocation of AssignmentsMetadataCacheRefreshTask
        Thread t1 = new Thread(task);
        t1.start();
        t1.join();

        //Now after task completion, Last refresh time should be the provided cTime2.
        assertThat(cTime2, is(task.getLastRefreshTime()));

    }

    @Test
    public void refreshCachePreventMultipleInvocations() throws InterruptedException {
        //Create mocks
        AssignmentsMetadataCache metadataCache = mock(AssignmentsMetadataCache.class);
        AssignmentMetadataCacheTimeService timeService = mock(AssignmentMetadataCacheTimeService.class);
        Date cTime1 = new Date();
        when(timeService.getCurrentTime()).thenReturn(cTime1);

        when(metadataCache.refresh()).thenAnswer(new Answer() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(1000);
                return Boolean.TRUE;
            }
        });

        AssignmentsMetadataCacheRefreshTask task1 = new AssignmentsMetadataCacheRefreshTask(metadataCache, timeService);

        //Actual invocation of AssignmentsMetadataCacheRefreshTask
        Thread t1 = new Thread(task1);
        t1.start();

        Thread t2 = new Thread(task1);
        t2.start();

        t1.join();
        t2.join();

        //Verify that refresh should be called only once - despite multiple threads trying to refresh cache
        verify(metadataCache, times(1)).refresh();

    }

}
