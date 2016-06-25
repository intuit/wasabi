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
package com.intuit.wasabi.eventlog.events;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link BucketCreateEvent}.
 */
public class BucketCreateEventTest {

    @Mock
    private Experiment experiment;

    @Mock
    private Experiment.Label experimentLabel;

    @Mock
    private UserInfo userInfo;

    @Mock
    private Bucket bucket;

    @Mock
    private Bucket.Label bucketLabel;

    @Mock
    private Application.Name appName;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(experiment.getApplicationName()).thenReturn(appName);
    }

    @Test
    public void testConstructor() throws Exception {
        try {
            new BucketCreateEvent(null, bucket);
            Assert.fail("BucketCreateEvent must not be creatable with a null experiment.");
        } catch (IllegalArgumentException ignored) {
        }

        try {
            new BucketCreateEvent(experiment, null);
            Assert.fail("BucketCreateEvent must not be creatable with a null bucket.");
        } catch (IllegalArgumentException ignored) {
        }

        BucketCreateEvent systemEvent = new BucketCreateEvent(experiment, bucket);
        Assert.assertEquals(experiment, systemEvent.getExperiment());
        Assert.assertEquals(bucket, systemEvent.getBucket());
        Assert.assertEquals(appName, systemEvent.getApplicationName());
        AbstractEventTest.testValidSystemEvent(systemEvent);

        BucketCreateEvent userEvent = new BucketCreateEvent(userInfo, experiment, bucket);
        Assert.assertEquals(experiment, userEvent.getExperiment());
        Assert.assertEquals(bucket, userEvent.getBucket());
        Assert.assertEquals(userInfo, userEvent.getUser());
        Assert.assertEquals(appName, systemEvent.getApplicationName());
    }


    @Test
    public void testGetDefaultDescription() throws Exception {
        Mockito.when(experiment.getLabel()).thenReturn(experimentLabel);
        Mockito.when(experimentLabel.toString()).thenReturn("UnitTestExperiment");
        Mockito.when(bucket.getLabel()).thenReturn(bucketLabel);
        Mockito.when(bucketLabel.toString()).thenReturn("UnitTestBucket");

        BucketCreateEvent systemEvent = new BucketCreateEvent(experiment, bucket);
        Assert.assertEquals(EventLog.SYSTEM_USER.getUsername() + " created bucket UnitTestExperiment.UnitTestBucket.", systemEvent.getDefaultDescription());
    }
}
