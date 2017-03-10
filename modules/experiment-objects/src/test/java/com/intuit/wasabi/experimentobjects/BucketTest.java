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
package com.intuit.wasabi.experimentobjects;

import com.intuit.wasabi.experimentobjects.exceptions.InvalidBucketStateTransitionException;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class BucketTest {

    final static double EPS = 0.000000000000001;
    final static Application.Name testApp = Application.Name.valueOf("testApp");

    @Test
    public void testBucket() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withLabel(Experiment.Label.valueOf("exp2")).withStartTime(c.getTime()).withSamplingPercent(1.0).build();
        Bucket a = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("green")).withAllocationPercent(1.0).withPayload(null).build();

        assertEquals(a.getLabel().toString(), "green");
        assertEquals(a.getExperimentID(), experiment.getID());
        assertEquals(a.getAllocationPercent(), 1.0, EPS);
        assertEquals(a.getPayload(), null);

    }

    @Test
    public void testIsControlDefaultBucket() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withLabel(Experiment.Label.valueOf("exp2")).withStartTime(c.getTime()).withSamplingPercent(1.0).build();
        Bucket a = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("green")).withAllocationPercent(1.0).withPayload(null).build();

        assertEquals(a.getLabel().toString(), "green");
        assertEquals(a.getExperimentID(), experiment.getID());
        assertEquals(a.getAllocationPercent(), 1.0, EPS);
        assertEquals(a.getPayload(), null);
        Bucket b = Bucket.from(a).build();
        assertEquals(a, b);

    }

    @Test
    public void testIsControlFalseBucket() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withLabel(Experiment.Label.valueOf("exp2")).withStartTime(c.getTime()).withSamplingPercent(1.0).build();
        Bucket a = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("green")).withAllocationPercent(1.0).withPayload(null).build();
        a.setControl(Boolean.FALSE);
        assertEquals(a.getLabel().toString(), "green");
        assertEquals(a.getExperimentID(), experiment.getID());
        assertEquals(a.getAllocationPercent(), 1.0, EPS);
        assertEquals(a.getPayload(), null);
        Bucket b = Bucket.from(a).build();
        assertEquals(a, b);

    }

    @Test
    public void testIsControlTrueBucket() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        Experiment experiment = Experiment.withID(Experiment.ID.newInstance()).withApplicationName(testApp)
                .withLabel(Experiment.Label.valueOf("exp2")).withStartTime(c.getTime()).withSamplingPercent(1.0).build();
        Bucket a = Bucket.newInstance(experiment.getID(), Bucket.Label.valueOf("green")).withAllocationPercent(1.0).withPayload(null).build();
        a.setControl(Boolean.TRUE);
        assertEquals(a.getLabel().toString(), "green");
        assertEquals(a.getExperimentID(), experiment.getID());
        assertEquals(a.getAllocationPercent(), 1.0, EPS);
        assertEquals(a.getPayload(), null);
        Bucket b = Bucket.from(a).build();
        assertEquals(a, b);

    }

    @Test(expected = InvalidBucketStateTransitionException.class)
    public void testBucketStateTransition() {
        ExperimentValidator validator = new ExperimentValidator();
        //valid transition
        validator.validateBucketStateTransition(Bucket.State.OPEN, Bucket.State.CLOSED);
        //not valid transition
        validator.validateBucketStateTransition(Bucket.State.CLOSED, Bucket.State.OPEN);

    }


}
