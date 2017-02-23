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
 * Tests for {@link BucketChangeEvent}.
 */
public class BucketChangeEventTest {
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
            new BucketChangeEvent(null, bucket, "label", "gren", "green");
            Assert.fail("BucketChangeEvent must not be creatable with a null experiment.");
        } catch (IllegalArgumentException ignored) {
        }

        try {
            new BucketChangeEvent(experiment, null, "label", "gren", "green");
            Assert.fail("BucketCreateEvent must not be creatable with a null bucket.");
        } catch (IllegalArgumentException ignored) {
        }

        BucketChangeEvent systemEvent = new BucketChangeEvent(experiment, bucket, "allocationPercent", "0.2", "0.25");
        Assert.assertEquals(experiment, systemEvent.getExperiment());
        Assert.assertEquals(bucket, systemEvent.getBucket());
        Assert.assertEquals(appName, systemEvent.getApplicationName());
        AbstractChangeEventTest.testValidSystemEvent(systemEvent, "allocationPercent", "0.2", "0.25");

        BucketChangeEvent userEvent = new BucketChangeEvent(userInfo, experiment, bucket, "label", "yellow", "orange");
        Assert.assertEquals(experiment, userEvent.getExperiment());
        Assert.assertEquals(bucket, userEvent.getBucket());
        Assert.assertEquals(userInfo, userEvent.getUser());
        Assert.assertEquals(appName, systemEvent.getApplicationName());
    }

    @Test
    public void testGetDefaultDescription() throws Exception {
//        Mockito.doReturn(experimentLabel).when(experiment).getLabel();
//        Mockito.doReturn("UnitTestExperiment").when(experimentLabel).toString();
//        Mockito.doReturn(bucketLabel).when(bucket).getLabel();
//        Mockito.doReturn("UnitTestBucket").when(bucketLabel).toString();
        Mockito.when(experiment.getLabel()).thenReturn(experimentLabel);
        Mockito.when(experimentLabel.toString()).thenReturn("UnitTestExperiment");
        Mockito.when(bucket.getLabel()).thenReturn(bucketLabel);
        Mockito.when(bucketLabel.toString()).thenReturn("UnitTestBucket");

        BucketChangeEvent systemEvent = new BucketChangeEvent(experiment, bucket, "label", "green", "blue");
        Assert.assertEquals(EventLog.SYSTEM_USER.getUsername() + " changed property label of bucket UnitTestExperiment.UnitTestBucket from green to blue.", systemEvent.getDefaultDescription());
    }
}
