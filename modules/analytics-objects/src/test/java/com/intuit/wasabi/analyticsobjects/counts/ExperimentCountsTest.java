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
package com.intuit.wasabi.analyticsobjects.counts;

import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.test.util.TestUtils;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExperimentCountsTest {
    Counts impressionCounts;
    Counts jointActionCounts;
    Map<Event.Name, ActionCounts> actionCounts;
    Map<Bucket.Label, BucketCounts> buckets;

    ExperimentCounts counter;

    @Before
    public void setup(){
        impressionCounts = new Counts.Builder().withEventCount(100).withUniqueUserCount(100).build();
        jointActionCounts = new Counts.Builder().withEventCount(200).withUniqueUserCount(200).build();
        actionCounts = new HashMap<Event.Name, ActionCounts>();
        buckets = new HashMap<Bucket.Label, BucketCounts>();

        counter = new ExperimentCounts.Builder().withJointActionCounts(jointActionCounts)
                .withImpressionCounts(impressionCounts).withActionCounts(actionCounts)
                .withBuckets(buckets).build();
    }

    @Test
    public void testBuilder(){
        assertEquals(counter.getActionCounts(), actionCounts);
        assertEquals(counter.getJointActionCounts(), jointActionCounts);
        assertEquals(counter.getImpressionCounts(), impressionCounts);
        assertEquals(counter.getBuckets(), buckets);

        assertNotNull(counter.toString());
        assertNotNull(counter.hashCode());
    }

    @Test
    public void testAddBucketCounts(){
        Bucket.Label label = Bucket.Label.valueOf("TestLabel");
        BucketCounts bucketCounter = new BucketCounts.Builder().withLabel(label)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();

        counter.addBucketCounts(label, bucketCounter);
        assertTrue(counter.getBuckets().containsKey(label));

        ExperimentCounts clonedExperimentCounter = counter.clone();
        assertTrue(clonedExperimentCounter.getBuckets().containsKey(label));

        counter.setBuckets(buckets);
        ExperimentCounts otherCounter = new ExperimentCounts.Builder().withJointActionCounts(jointActionCounts)
                .withImpressionCounts(impressionCounts).withActionCounts(actionCounts)
                .withBuckets(buckets).build();
    }

    @Test
    public void testCloneWithEmptyBucket(){
        assertEquals(0, counter.getBuckets().size());

        ExperimentCounts clonedExperimentCount = counter.clone();
        assertEquals(0, clonedExperimentCount.getBuckets().size());

    }

    @Test
    public void testCloneWithOneBucket(){
        Bucket.Label label = Bucket.Label.valueOf("TestLabel");
        BucketCounts bucketCounts = new BucketCounts.Builder().withLabel(label)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();

        counter.addBucketCounts(label, bucketCounts);
        assertEquals(1, counter.getBuckets().size());

        ExperimentCounts clonedExperimentCount = counter.clone();
        assertEquals(1, clonedExperimentCount.getBuckets().size());
        
        TestUtils.assertMapsEqual(counter.getBuckets(), clonedExperimentCount.getBuckets());

    }

    @Test
    public void testCloneWithTwoBucket(){
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
        assertEquals(2, counter.getBuckets().size());

        ExperimentCounts clonedExperimentCount = counter.clone();
        assertEquals(2, clonedExperimentCount.getBuckets().size());
        TestUtils.assertMapsEqual(counter.getBuckets(), clonedExperimentCount.getBuckets());
    }

    @Test
    public void testEqualsWithSelfAndClone(){
        Bucket.Label label = Bucket.Label.valueOf("TestLabel");
        BucketCounts bucketCounts = new BucketCounts.Builder().withLabel(label)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();

        counter.addBucketCounts(label, bucketCounts);
        assertEquals(counter, counter.clone());
        assertEquals(counter, counter);

    }

    @Test
    public void testEqualsTwoCopies(){

        ExperimentCounts otherCounter = new ExperimentCounts.Builder().withJointActionCounts(jointActionCounts)
                .withImpressionCounts(impressionCounts).withActionCounts(actionCounts)
                .withBuckets(buckets).build();
        assertEquals(counter, otherCounter.clone());
        assertEquals(counter, otherCounter);

    }

    @Test
    public void testNotEquals(){

        ExperimentCounts otherCounter = new ExperimentCounts.Builder().withJointActionCounts(jointActionCounts)
                .withImpressionCounts(impressionCounts)
                .withBuckets(buckets).build();
        assertTrue(!counter.equals(otherCounter));

    }
}
