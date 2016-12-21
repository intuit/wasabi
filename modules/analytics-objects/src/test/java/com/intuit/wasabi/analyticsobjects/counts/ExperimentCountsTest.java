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
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;

/**
 * This class tests the {@link ExperimentCounts}.
 */
public class ExperimentCountsTest {

    private Counts impressionCounts;
    private Counts jointActionCounts;
    private Map<Event.Name, ActionCounts> actionCounts;
    private Map<Bucket.Label, BucketCounts> buckets;

    private ExperimentCounts counter;

    @Before
    public void setup() {
        impressionCounts = new Counts.Builder().withEventCount(100).withUniqueUserCount(100).build();
        jointActionCounts = new Counts.Builder().withEventCount(200).withUniqueUserCount(200).build();
        actionCounts = new HashMap<>();
        buckets = new HashMap<>();

        counter = new ExperimentCounts.Builder().withJointActionCounts(jointActionCounts)
                .withImpressionCounts(impressionCounts).withActionCounts(actionCounts)
                .withBuckets(buckets).build();
    }

    @Test
    public void testBuilder() {
        assertThat(counter.getActionCounts(), equalTo(actionCounts));
        assertThat(counter.getJointActionCounts(), equalTo(jointActionCounts));
        assertThat(counter.getImpressionCounts(), equalTo(impressionCounts));
        assertThat(counter.getBuckets(), equalTo(buckets));

        String counterString = counter.toString();
        assertThat(counterString, containsString(actionCounts.toString()));
        assertThat(counterString, containsString(impressionCounts.toString()));
        assertThat(counterString, containsString(jointActionCounts.toString()));
        assertThat(counterString, containsString(buckets.toString()));

        assertThat(counter.hashCode(), equalTo(counter.clone().hashCode()));
    }

    @Test
    public void testAddBucketCounts() {
        Bucket.Label label = Bucket.Label.valueOf("TestLabel");
        BucketCounts bucketCounter = new BucketCounts.Builder().withLabel(label)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();

        counter.addBucketCounts(label, bucketCounter);
        assertThat(counter.getBuckets(), hasKey(label));

        ExperimentCounts clonedExperimentCounter = counter.clone();
        assertThat(clonedExperimentCounter.getBuckets(), hasKey(label));

        counter.setBuckets(buckets);
    }

    @Test
    public void testCloneWithEmptyBucket() {
        assertThat(0, is(counter.getBuckets().size()));
        ExperimentCounts clonedExperimentCount = counter.clone();
        assertThat(0, is(clonedExperimentCount.getBuckets().size()));
    }

    @Test
    public void testCloneWithOneBucket() {
        Bucket.Label label = Bucket.Label.valueOf("TestLabel");
        BucketCounts bucketCounts = new BucketCounts.Builder().withLabel(label)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();

        counter.addBucketCounts(label, bucketCounts);
        assertThat(1, is(counter.getBuckets().size()));

        ExperimentCounts clonedExperimentCount = counter.clone();
        assertThat(1, is(clonedExperimentCount.getBuckets().size()));
        assertThat(counter.getBuckets().entrySet(), equalTo(clonedExperimentCount.getBuckets().entrySet()));
    }

    @Test
    public void testCloneWithTwoBucket() {
        Bucket.Label label = Bucket.Label.valueOf("TestLabel");
        BucketCounts bucketCounts = new BucketCounts.Builder().withLabel(label)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();

        counter.addBucketCounts(label, bucketCounts);
        Bucket.Label label2 = Bucket.Label.valueOf("TestLabel2");
        BucketCounts bucketCounts2 = new BucketCounts.Builder().withLabel(label2)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();

        counter.addBucketCounts(label2, bucketCounts2);
        assertThat(2, is(counter.getBuckets().size()));

        ExperimentCounts clonedExperimentCount = counter.clone();
        assertThat(2, is(clonedExperimentCount.getBuckets().size()));
        assertThat(counter.getBuckets().entrySet(), equalTo(clonedExperimentCount.getBuckets().entrySet()));
    }

    @Test
    public void testEqualsWithClone() {
        Bucket.Label label = Bucket.Label.valueOf("TestLabel");
        BucketCounts bucketCounts = new BucketCounts.Builder().withLabel(label)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();

        counter.addBucketCounts(label, bucketCounts);
        assertThat(counter, equalTo(counter.clone()));

    }

    @Test
    public void testEqualsTwoCopies() {
        ExperimentCounts otherCounter = new ExperimentCounts.Builder().withJointActionCounts(jointActionCounts)
                .withImpressionCounts(impressionCounts).withActionCounts(actionCounts)
                .withBuckets(buckets).build();
        assertThat(counter, equalTo(otherCounter.clone()));
        assertThat(counter, equalTo(otherCounter));

    }

    @Test
    public void testNotEquals() {
        ExperimentCounts otherCounter = new ExperimentCounts.Builder().withJointActionCounts(jointActionCounts)
                .withImpressionCounts(impressionCounts)
                .withBuckets(buckets).build();
        assertThat(counter, not(equalTo(otherCounter)));

    }
}
