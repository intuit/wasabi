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
package com.intuit.wasabi.analyticsobjects.counts;

import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.Label;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

/**
 * This class tests the {@link BucketCounts}.
 */
public class BucketCountsTest {

    private Bucket.Label label;
    private Counts impressionCounts;
    private Counts jointActionCounts;
    private Map<Event.Name, ActionCounts> actionCounts;
    private BucketCounts counter;

    @Before
    public void setup() {
        label = Bucket.Label.valueOf("TestLabel");
        impressionCounts = new Counts.Builder().withEventCount(100).withUniqueUserCount(100).build();
        jointActionCounts = new Counts.Builder().withEventCount(200).withUniqueUserCount(200).build();
        actionCounts = new HashMap<>();
        counter = new BucketCounts.Builder().withLabel(label)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();
    }

    @Test
    public void testBuilder() {
        assertThat(counter.getLabel(), equalTo(label));
        assertThat(counter.getActionCounts(), equalTo(actionCounts));
        assertThat(counter.getJointActionCounts(), equalTo(jointActionCounts));
        assertThat(counter.getImpressionCounts(), equalTo(impressionCounts));

        String counterString = counter.toString();
        assertThat(counterString, containsString("eventCount=100"));
        assertThat(counterString, containsString("uniqueUserCount=100"));
        assertThat(counterString, containsString("eventCount=200"));
        assertThat(counterString, containsString("uniqueUserCount=200"));
    }

    @Test
    public void testCloneAndHashCode() {
        BucketCounts countClone = counter.clone();
        assertThat(counter.getLabel(), equalTo(countClone.getLabel()));
        assertThat(counter.getActionCounts(), equalTo(countClone.getActionCounts()));
        assertThat(counter.getImpressionCounts(), equalTo(countClone.getImpressionCounts()));
        assertThat(counter.getJointActionCounts(), equalTo(countClone.getJointActionCounts()));

        assertThat(counter.hashCode(), equalTo(countClone.hashCode()));
        countClone.setImpressionCounts(new Counts(42, 42));
        assertThat(counter.hashCode(), not(equalTo(countClone.hashCode())));
    }

    @Test
    public void testEqualsTwoInstances() {
        BucketCounts counter2 = new BucketCounts.Builder().withLabel(label)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();
        assertThat(counter, equalTo(counter2));
        assertThat(counter, equalTo(counter2.clone()));
    }

    @Test
    public void testNotEqualsTwoInstances() {
        Label label2 = Bucket.Label.valueOf("TestLabel2");
        BucketCounts counter2 = new BucketCounts.Builder().withLabel(label2)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();
        assertThat(counter, not(equalTo(counter2)));
        assertThat(counter, not(equalTo(counter2.clone())));
    }

    @Test
    public void testBuilderBasedOn() {
        BucketCounts buildBasedOn = new BucketCounts.Builder().basedOn(counter).build();
        buildBasedOn.setLabel(label);
        assertThat(buildBasedOn.getLabel(), equalTo(label));
        assertThat(buildBasedOn.getActionCounts(), equalTo(actionCounts));
        assertThat(buildBasedOn.getJointActionCounts(), equalTo(jointActionCounts));
        assertThat(buildBasedOn.getImpressionCounts(), equalTo(impressionCounts));
    }
}
