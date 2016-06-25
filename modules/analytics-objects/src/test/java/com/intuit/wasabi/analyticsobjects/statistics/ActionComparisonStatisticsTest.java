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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.*;

public class ActionComparisonStatisticsTest {
    Event.Name actionName;
    boolean sufficientData;
    Double fractionDataCollected;
    Bucket.Label clearComparisonWinner;
    Estimate actionRateDifference;
    DistinguishableEffectSize smallestDistinguishableEffectSize;
    ActionComparisonStatistics actionComparisonStatistics;

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
        assertEquals(actionComparisonStatistics.getActionName(), actionName);


        ComparisonStatistics statistics = new ComparisonStatistics.Builder().withActionRateDifference(actionRateDifference)
                .withClearComparisonWinner(clearComparisonWinner).withFractionDataCollected(fractionDataCollected)
                .withSmallestDistinguishableEffectSize(smallestDistinguishableEffectSize).withSufficientData(sufficientData).build();
        ActionComparisonStatistics otherActionComparisonStatistics = new ActionComparisonStatistics.Builder().withActionName(actionName)
                .withComparisonStatistic(statistics).build();

        assertNotNull(actionComparisonStatistics.hashCode());
        assertNotNull(actionComparisonStatistics.toString());
        assertNotNull(actionComparisonStatistics.clone());
        assertTrue(actionComparisonStatistics.equals(otherActionComparisonStatistics));
        assertTrue(actionComparisonStatistics.equals(actionComparisonStatistics));
        assertFalse(actionComparisonStatistics.equals(null));
        Assert.assertFalse(actionComparisonStatistics.equals(statistics));
    }

    @Test
    public void testSettersAndGetters() {
        actionComparisonStatistics.setActionName(null);
        assertEquals(actionComparisonStatistics.getActionName(), null);
    }
}
