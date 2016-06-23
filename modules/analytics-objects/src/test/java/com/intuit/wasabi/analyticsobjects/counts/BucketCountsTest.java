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
import com.intuit.wasabi.experimentobjects.Bucket.Label;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BucketCountsTest {
    Bucket.Label label;
    Counts impressionCounts;
    Counts jointActionCounts;
    Map<Event.Name, ActionCounts> actionCounts;
    BucketCounts counter;

    @Before
    public void setup(){
        label = Bucket.Label.valueOf("TestLabel");
        impressionCounts = new Counts.Builder().withEventCount(100).withUniqueUserCount(100).build();
        jointActionCounts = new Counts.Builder().withEventCount(200).withUniqueUserCount(200).build();
        actionCounts = new HashMap<Event.Name, ActionCounts>();
        counter = new BucketCounts.Builder().withLabel(label)
                                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                                .withActionCounts(actionCounts).build();
    }

    @Test
    public void testBuilder(){
        assertEquals(counter.getLabel(), label);
        assertEquals(counter.getActionCounts(), actionCounts);
        assertEquals(counter.getJointActionCounts(), jointActionCounts);
        assertEquals(counter.getImpressionCounts(), impressionCounts);

        assertNotNull(counter.toString());
        assertNotNull(counter.hashCode());
        assertNotNull(counter.clone());
    }

    @Test
    public void testEqualsWithSelfAndClone(){
    	assertEquals(counter, counter);
    	assertEquals(counter, counter.clone());
    }

    @Test
    public void testEqualsTwoInstances(){
        BucketCounts counter2 = new BucketCounts.Builder().withLabel(label)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();
    	assertEquals(counter, counter2);
    	assertEquals(counter, counter2.clone());
    }

    @Test
    public void testNotEqualsTwoInstances(){
        Label label2 = Bucket.Label.valueOf("TestLabel2");
        BucketCounts counter2 = new BucketCounts.Builder().withLabel(label2)
                .withJointActionCounts(jointActionCounts).withImpressionCounts(impressionCounts)
                .withActionCounts(actionCounts).build();
    	assertTrue(!counter.equals(counter2));
    	assertTrue(!counter.equals(counter2.clone()));
    }

    @Test
    public void testBuilderBasedOn(){
        BucketCounts buildBasedOn =  new BucketCounts.Builder().basedOn(counter).build();
        buildBasedOn.setLabel(label);
        assertEquals(buildBasedOn.getLabel(),label);
        assertEquals(buildBasedOn.getActionCounts(), actionCounts);
        assertEquals(buildBasedOn.getJointActionCounts(), jointActionCounts);
        assertEquals(buildBasedOn.getImpressionCounts(), impressionCounts);
    }
}
