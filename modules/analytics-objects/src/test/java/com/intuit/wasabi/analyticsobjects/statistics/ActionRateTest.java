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
package com.intuit.wasabi.analyticsobjects.statistics;

import com.intuit.wasabi.analyticsobjects.Event;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link ActionRate}.
 */
public class ActionRateTest {

    private Double estimate;
    private Double lowerBound;
    private Double upperBound;
    private Event.Name actionName;
    private ActionRate actionRate;

    @Before
    public void setup(){
        actionName = Event.Name.valueOf("TestAction");
        estimate = 0.5;
        lowerBound = 0.45;
        upperBound = 0.55;
        actionRate = new ActionRate.Builder().withActionName(actionName).withEstimateValue(estimate)
                    .withEstimateLower(lowerBound).withEstimateUpper(upperBound).build();
    }

    @Test
    public void testBuilder(){
        assertEquals(actionRate.getActionName(), actionName);

        Estimate estimator = new Estimate.Builder().withEstimate(estimate).withLowerBound(lowerBound)
                .withUpperBound(upperBound).build();
        ActionRate otherActionRate = new ActionRate.Builder().withActionName(actionName).withEstimate(estimator).build();

        String actRate = actionRate.toString();
        assertTrue(actRate.contains(actionName.toString()));
        assertTrue(actRate.contains(estimate.toString()));
        assertTrue(actRate.contains(lowerBound.toString()));
        assertTrue(actRate.contains(upperBound.toString()));

        assertEquals(actionRate.hashCode(), actionRate.clone().hashCode());

        assertTrue(actionRate.equals(otherActionRate));
        assertTrue(actionRate.equals(actionRate));
        assertFalse(actionRate.equals(null));
        assertFalse(actionRate.equals(estimator));
    }

    @Test
    public void testSettersandGetters(){
        actionRate.setActionName(Event.Name.valueOf("TestOtherAction"));
        assertEquals(actionRate.getActionName(), Event.Name.valueOf("TestOtherAction"));
    }

}
