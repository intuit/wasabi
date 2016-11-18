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

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for the {@link BucketComparison}
 */
public class BucketComparisonTest {

    BucketComparison bucketComparison;

    @Before
    public void setup() {
        Bucket.Label label = Bucket.Label.valueOf("TestLabel");
        Map<Event.Name, ActionComparisonStatistics> map = new HashMap<Event.Name, ActionComparisonStatistics>();
        ComparisonStatistics comparisonStatistics = new ComparisonStatistics();
        bucketComparison = new BucketComparison.Builder().withOtherLabel(label)
                .withJointActionComparison(comparisonStatistics).withActionComparisons(map).build();
    }

    @Test
    public void gettersAndSetters() {
        Bucket.Label label = Bucket.Label.valueOf("NewTestLabel");
        Map<Event.Name, ActionComparisonStatistics> map = new HashMap<Event.Name, ActionComparisonStatistics>();
        ComparisonStatistics comparisonStatistics = new ComparisonStatistics();
        bucketComparison.setActionComparisons(map);
        bucketComparison.setJointActionComparison(comparisonStatistics);
        bucketComparison.setOtherLabel(label);

        assertThat(bucketComparison.getActionComparisons(), is(map));
        assertThat(bucketComparison.getJointActionComparison(), is(comparisonStatistics));
        assertThat(bucketComparison.getOtherLabel(), is(label));
    }

    @Test
    public void addToActionComparisonsTest() {
        Bucket.Label label = Bucket.Label.valueOf("TestLabel");
        ComparisonStatistics comparisonStatistics = new ComparisonStatistics();
        BucketComparison bucketComparison = new BucketComparison.Builder().withOtherLabel(label)
                .withJointActionComparison(comparisonStatistics).withActionComparisons(null).build();
        bucketComparison.addToActionComparisons(Event.Name.valueOf("TestName"), new ActionComparisonStatistics());

        assertThat(bucketComparison.getActionComparisons().size(), is(1));
    }

    @Test
    public void toStringTest() {
        assertThat(bucketComparison.toString(), is("BucketComparison"
                + "[otherLabel=TestLabel,jointActionComparison="
                + "ComparisonStatistics[sufficientData=false,fractionDataCollected=<null>,"
                + "clearComparisonWinner=<null>,actionRateDifference=<null>,"
                + "smallestDistinguishableEffectSize=<null>],actionComparisons={}]"));
    }

    @Test
    public void hashcodeTest() {
        assertThat(bucketComparison.hashCode(), is(-214943778));
    }

    @Test
    public void cloneAndEqual() {
        BucketComparison bucketComparison1 = bucketComparison.clone();
        assertThat(bucketComparison1, is(bucketComparison1));
    }

}
