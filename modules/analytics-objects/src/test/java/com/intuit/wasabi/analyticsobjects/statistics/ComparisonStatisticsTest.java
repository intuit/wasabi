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

import com.intuit.wasabi.experimentobjects.Bucket;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ComparisonStatisticsTest {
    boolean sufficientData;
    Double fractionDataCollected;
    Bucket.Label clearComparisonWinner;
    Estimate actionRateDifference;
    DistinguishableEffectSize smallestDistinguishableEffectSize;
    ComparisonStatistics statistics;

    @Before
    public void setup(){
        clearComparisonWinner = Bucket.Label.valueOf("TestWinner");
        sufficientData = true;
        fractionDataCollected = 0.5;
        actionRateDifference = new Estimate();
        smallestDistinguishableEffectSize = new DistinguishableEffectSize();
        statistics = new ComparisonStatistics.Builder().withActionRateDifference(actionRateDifference)
                        .withClearComparisonWinner(clearComparisonWinner).withFractionDataCollected(fractionDataCollected)
                        .withSmallestDistinguishableEffectSize(smallestDistinguishableEffectSize).withSufficientData(sufficientData).build();
    }

    @Test
    public void testBuilder(){
        assertEquals(statistics.getFractionDataCollected(), fractionDataCollected);
        assertEquals(statistics.isSufficientData(), sufficientData);
        assertEquals(statistics.getClearComparisonWinner(), clearComparisonWinner);
        assertEquals(statistics.getActionRateDifference(), actionRateDifference);
        assertEquals(statistics.getSmallestDistinguishableEffectSize(), smallestDistinguishableEffectSize);

        assertTrue(statistics.equals(statistics.clone()));
        assertTrue(statistics.equals(statistics));
        assertFalse(statistics.equals(null));
        assertFalse(statistics.equals(fractionDataCollected));
    }

    @Test
    public void testSettersAndGetters(){
        fractionDataCollected = 0.0;
        statistics.setFractionDataCollected(fractionDataCollected);
        assertEquals(statistics.getFractionDataCollected(), fractionDataCollected);

        statistics.setSufficientData(!sufficientData);
        assertEquals(statistics.isSufficientData(), !sufficientData);

        clearComparisonWinner = Bucket.Label.valueOf("TestOtherWinner");
        statistics.setClearComparisonWinner(clearComparisonWinner);
        assertEquals(statistics.getClearComparisonWinner(), clearComparisonWinner);

        statistics.setActionRateDifference(null);
        assertEquals(statistics.getActionRateDifference(), null);

        statistics.setSmallestDistinguishableEffectSize(null);
        assertEquals(statistics.getSmallestDistinguishableEffectSize(), null);

    }
}
