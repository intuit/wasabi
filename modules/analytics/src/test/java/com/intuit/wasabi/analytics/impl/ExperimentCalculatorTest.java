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
package com.intuit.wasabi.analytics.impl;

import com.intuit.wasabi.analytics.AnalysisTools;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.counts.ActionCounts;
import com.intuit.wasabi.analyticsobjects.counts.BucketCounts;
import com.intuit.wasabi.analyticsobjects.counts.Counts;
import com.intuit.wasabi.analyticsobjects.counts.ExperimentCounts;
import com.intuit.wasabi.experimentobjects.Bucket;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentCalculatorTest {

    private Map<Bucket.Label, BucketCounts> buckets;
    private Map<Event.Name, ActionCounts> actions;
    private Event.Name clickName = Event.Name.valueOf("click");
    private Event.Name purchaseName = Event.Name.valueOf("purchase");
    private Event.Name commentName = Event.Name.valueOf("comment");
    private Bucket.Label bucket1Label = Bucket.Label.valueOf("one");
    private Bucket.Label bucket2Label = Bucket.Label.valueOf("two");
    private AnalysisTools analysisTools = new AnalysisToolsImpl();

    @Before
    public void before() {
        buckets = new HashMap<>();
        actions = new HashMap<>();

        Map<Event.Name, ActionCounts> actions1 = new HashMap<>();
        Map<Event.Name, ActionCounts> actions2 = new HashMap<>();
        ActionCounts click1 = new ActionCounts.Builder().withActionName(clickName).withEventCount(7)
                .withUniqueUserCount(3).build();
        ActionCounts click2 = new ActionCounts.Builder().withActionName(clickName).withEventCount(9)
                .withUniqueUserCount(6).build();
        ActionCounts purchase1 = new ActionCounts.Builder().withActionName(purchaseName).withEventCount(3)
                .withUniqueUserCount(1).build();
        ActionCounts purchase2 = new ActionCounts.Builder().withActionName(purchaseName).withEventCount(3)
                .withUniqueUserCount(2).build();
        ActionCounts comment1 = new ActionCounts.Builder().withActionName(commentName).withEventCount(2)
                .withUniqueUserCount(2).build();

        actions1.put(clickName, click1);
        actions1.put(purchaseName, purchase1);
        actions1.put(commentName, comment1);
        actions2.put(clickName, click2);
        actions2.put(purchaseName, purchase2);

        actions.put(clickName,
                new ActionCounts.Builder().withActionName(clickName).withEventCount(16).withUniqueUserCount(9).build());
        actions.put(purchaseName,
                new ActionCounts.Builder().withActionName(purchaseName).withEventCount(6).withUniqueUserCount(3).build());
        actions.put(commentName,
                new ActionCounts.Builder().withActionName(commentName).withEventCount(2).withUniqueUserCount(2).build());

        BucketCounts bucket1 = new BucketCounts.Builder().withLabel(bucket1Label)
                .withImpressionCounts(new Counts.Builder().withEventCount(15)
                        .withUniqueUserCount(11).build())
                .withJointActionCounts(new Counts.Builder().withEventCount(12)
                        .withUniqueUserCount(3).build())
                .withActionCounts(actions1).build();
        BucketCounts bucket2 = new BucketCounts.Builder().withLabel(bucket2Label)
                .withImpressionCounts(new Counts.Builder().withEventCount(19)
                        .withUniqueUserCount(8).build())
                .withJointActionCounts(new Counts.Builder().withEventCount(12)
                        .withUniqueUserCount(7).build())
                .withActionCounts(actions2).build();
        buckets.put(bucket1Label, bucket1);
        buckets.put(bucket2Label, bucket2);
    }

    @Test
    public void calculateExperimentCounts() throws Exception {
        ExperimentCounts counts = analysisTools.calculateExperimentCounts(buckets.values());

        assertThat("test bucket counts", counts.getBuckets(), is(buckets));
        assertThat("total impressions = 34", counts.getImpressionCounts().getEventCount(), is(34L));
        assertThat("unique impressions = 19", counts.getImpressionCounts().getUniqueUserCount(), is(19L));
        assertThat("total joint actions = 24", counts.getJointActionCounts().getEventCount(), is(24L));
        assertThat("unique joint actions = 10", counts.getJointActionCounts().getUniqueUserCount(), is(10L));
        assertThat("action count list", counts.getActionCounts(), is(actions));
    }
}
