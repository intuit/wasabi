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

import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.ExperimentBase;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 */
public class BucketDeleteEventTest {

    @Test
    public void testGetters() throws Exception {
        Bucket buck = Mockito.mock(Bucket.class);
        ExperimentBase exp = Mockito.mock(Experiment.class);
        Application.Name appName = Mockito.mock(Application.Name.class);
        Mockito.when(exp.getApplicationName()).thenReturn(appName);
        BucketDeleteEvent bde = new BucketDeleteEvent(exp, buck);
        Assert.assertEquals(exp, bde.getExperiment());
        Assert.assertEquals(buck, bde.getBucket());
        Assert.assertEquals(appName, bde.getApplicationName());
    }

    @Test
    public void testGetDefaultDescription() throws Exception {
        AbstractEventTest.testValidSystemEvent(new BucketDeleteEvent(Mockito.mock(ExperimentBase.class), Mockito.mock(Bucket.class)));
    }
}
