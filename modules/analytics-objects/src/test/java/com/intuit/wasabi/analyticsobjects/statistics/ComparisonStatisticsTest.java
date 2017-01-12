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

import com.intuit.wasabi.experimentobjects.Bucket;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests the {@link ComparisonStatistics}.
 */
public class ComparisonStatisticsTest {

    private boolean sufficientData;
    private Double fractionDataCollected;
    private Bucket.Label clearComparisonWinner;
    private Estimate actionRateDifference;
    private DistinguishableEffectSize smallestDistinguishableEffectSize;
    private ComparisonStatistics statistics;

    @Before
    public void setup() {
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
    public void testBuilder() {
        assertThat(statistics.getFractionDataCollected(), equalTo(fractionDataCollected));
        assertThat(statistics.isSufficientData(), equalTo(sufficientData));
        assertThat(statistics.getClearComparisonWinner(), equalTo(clearComparisonWinner));
        assertThat(statistics.getActionRateDifference(), is(actionRateDifference));
        assertThat(statistics.getSmallestDistinguishableEffectSize(), is(smallestDistinguishableEffectSize));

        assertThat(statistics.hashCode(), is(statistics.clone().hashCode()));

        String stats = statistics.toString();
        assertThat(stats, containsString(fractionDataCollected.toString()));
        assertThat(stats, containsString(String.valueOf(sufficientData)));
        assertThat(stats, containsString(clearComparisonWinner.toString()));
        assertThat(stats, containsString(actionRateDifference.toString()));
        assertThat(stats, containsString(smallestDistinguishableEffectSize.toString()));

        assertThat(statistics, equalTo(statistics.clone()));
        assertThat(statistics, equalTo(statistics));
        assertThat(statistics, not(equalTo(null)));
        assertThat(statistics, not(equalTo(fractionDataCollected)));
    }

    @Test
    public void testSettersAndGetters() {
        fractionDataCollected = 0.0;
        statistics.setFractionDataCollected(fractionDataCollected);
        assertThat(statistics.getFractionDataCollected(), equalTo(fractionDataCollected));

        statistics.setSufficientData(!sufficientData);
        assertThat(statistics.isSufficientData(), not(sufficientData));

        clearComparisonWinner = Bucket.Label.valueOf("TestOtherWinner");
        statistics.setClearComparisonWinner(clearComparisonWinner);
        assertThat(statistics.getClearComparisonWinner(), is(clearComparisonWinner));

        statistics.setActionRateDifference(null);
        assertThat(statistics.getActionRateDifference(), nullValue());

        statistics.setSmallestDistinguishableEffectSize(null);
        assertThat(statistics.getSmallestDistinguishableEffectSize(), nullValue());

    }
}
