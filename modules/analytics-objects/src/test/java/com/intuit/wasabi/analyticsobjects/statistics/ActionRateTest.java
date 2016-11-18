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
package com.intuit.wasabi.analyticsobjects.statistics;

import com.intuit.wasabi.analyticsobjects.Event;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

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
    public void setup() {
        actionName = Event.Name.valueOf("TestAction");
        estimate = 0.5;
        lowerBound = 0.45;
        upperBound = 0.55;
        actionRate = new ActionRate.Builder().withActionName(actionName).withEstimateValue(estimate)
                .withEstimateLower(lowerBound).withEstimateUpper(upperBound).build();
    }

    @Test
    public void testBuilder() {
        assertThat(actionRate.getActionName(), equalTo(actionName));

        Estimate estimator = new Estimate.Builder().withEstimate(estimate).withLowerBound(lowerBound)
                .withUpperBound(upperBound).build();
        ActionRate otherActionRate = new ActionRate.Builder().withActionName(actionName).withEstimate(estimator).build();

        String actRate = actionRate.toString();
        assertThat(actRate, containsString(actionName.toString()));
        assertThat(actRate, containsString(estimate.toString()));
        assertThat(actRate, containsString(lowerBound.toString()));
        assertThat(actRate, containsString(upperBound.toString()));

        assertThat(actionRate.hashCode(), is(actionRate.clone().hashCode()));

        assertThat(actionRate, equalTo(otherActionRate));
        assertThat(actionRate, equalTo(actionRate));
        assertThat(actionRate, not(equalTo(null)));
        assertThat(actionRate, not(equalTo(estimator)));
    }

    @Test
    public void testSettersandGetters() {
        actionRate.setActionName(Event.Name.valueOf("TestOtherAction"));
        assertThat(actionRate.getActionName(), equalTo(Event.Name.valueOf("TestOtherAction")));
    }

}
