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
import com.intuit.wasabi.experimentobjects.Bucket;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests the {@link ActionComparisonStatistics}.
 */
public class ActionComparisonStatisticsTest {

    private Event.Name actionName;
    private boolean sufficientData;
    private Double fractionDataCollected;
    private Bucket.Label clearComparisonWinner;
    private Estimate actionRateDifference;
    private DistinguishableEffectSize smallestDistinguishableEffectSize;
    private ActionComparisonStatistics actionComparisonStatistics;

    @Before
    public void setup() {
        actionName = Event.Name.valueOf("TestAction");
        clearComparisonWinner = Bucket.Label.valueOf("TestWinner");
        sufficientData = true;
        fractionDataCollected = 0.5;
        actionRateDifference = new Estimate();
        smallestDistinguishableEffectSize = new DistinguishableEffectSize();
        actionComparisonStatistics = new ActionComparisonStatistics.Builder().withActionName(actionName)
                .withActionRateDifference(actionRateDifference)
                .withClearComparisonWinner(clearComparisonWinner).withFractionDataCollected(fractionDataCollected)
                .withSmallestDistinguishableEffectSize(smallestDistinguishableEffectSize).withSufficientData(sufficientData).build();
    }

    @Test
    public void testBuilder() {
        assertThat(actionComparisonStatistics.getActionName(), equalTo(actionName));


        ComparisonStatistics statistics = new ComparisonStatistics.Builder()
                .withActionRateDifference(actionRateDifference)
                .withClearComparisonWinner(clearComparisonWinner).withFractionDataCollected(fractionDataCollected)
                .withSmallestDistinguishableEffectSize(smallestDistinguishableEffectSize)
                .withSufficientData(sufficientData).build();

        ActionComparisonStatistics otherActionComparisonStatistics = new ActionComparisonStatistics.Builder()
                .withActionName(actionName)
                .withComparisonStatistic(statistics).build();

        String actionCompare = actionComparisonStatistics.toString();
        assertThat(actionCompare, containsString(clearComparisonWinner.toString()));
        assertThat(actionCompare, containsString(actionName.toString()));
        assertThat(actionCompare, containsString(actionRateDifference.toString()));
        assertThat(actionCompare, containsString(fractionDataCollected.toString()));
        assertThat(actionCompare, containsString(smallestDistinguishableEffectSize.toString()));
        assertThat(actionCompare, containsString(actionComparisonStatistics.toString()));

        assertThat(actionComparisonStatistics.hashCode(), equalTo(actionComparisonStatistics.clone().hashCode()));
        assertThat(actionComparisonStatistics, equalTo(otherActionComparisonStatistics));
        assertThat(actionComparisonStatistics, equalTo(actionComparisonStatistics));
        assertThat(actionComparisonStatistics, not(equalTo(statistics)));
    }

    @Test
    public void testSettersAndGetters() {
        actionComparisonStatistics.setActionName(null);
        assertThat(actionComparisonStatistics.getActionName(), nullValue());

        actionComparisonStatistics.setActionRateDifference(null);
        assertThat(actionComparisonStatistics.getActionRateDifference(), nullValue());
    }
}
