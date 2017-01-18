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
package com.intuit.wasabi.repository.cassandra.impl;

import com.intuit.hyrule.Rule;
import com.intuit.hyrule.RuleBuilder;
import com.intuit.wasabi.assignmentobjects.RuleCache;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExperimentRuleCacheUpdateEnvelopeTest {

    private final static Application.Name testApp = Application.Name.valueOf("testApp");

    @Mock
    private static RuleCache ruleCache;
    private static ExperimentRuleCacheUpdateEnvelope experimentRuleCacheUpdateEnvelope;

    @Test
    public void makeRuleTest() {

    }

    @Test
    public void updateRuleCacheTest() {
        //Create Experiments for App
        final Experiment.ID EXPERIMENT_ID = Experiment.ID.newInstance();
        Experiment experiment = Experiment.withID(EXPERIMENT_ID).withApplicationName(testApp).build();
        experiment.setRule("state=CA");
        final Rule RULE = new RuleBuilder().parseExpression(experiment.getRule());

        experimentRuleCacheUpdateEnvelope = new ExperimentRuleCacheUpdateEnvelope(experiment.getRule(), ruleCache,
                EXPERIMENT_ID) {
            @Override
            public Rule getExperimentRule(String anyString) {
                return RULE;
            }
        };
        Mockito.when(ruleCache.containsRule(experiment.getID())).thenReturn(false);
        // Expecting to run the ruleCache.setRule the first time
        experimentRuleCacheUpdateEnvelope.run();
        Mockito.verify(ruleCache, Mockito.times(1)).setRule(experiment.getID(), RULE);

        Mockito.when(ruleCache.containsRule(EXPERIMENT_ID)).thenReturn(true);
        Mockito.when(ruleCache.getRule(EXPERIMENT_ID)).thenReturn(new RuleBuilder().parseExpression("a=b"));
        // Expecting to run the ruleCache.setRule the second time
        experimentRuleCacheUpdateEnvelope.run();
        // Confirm that the setRule has been executed twice with the correct rule as value
        Mockito.verify(ruleCache, Mockito.times(2)).setRule(EXPERIMENT_ID, RULE);


        experimentRuleCacheUpdateEnvelope = new ExperimentRuleCacheUpdateEnvelope(null, ruleCache, EXPERIMENT_ID) {
            @Override
            public Rule getExperimentRule(String anyString) {
                return RULE;
            }
        };
        experiment.setRule(null);
        Mockito.when(ruleCache.getRule(EXPERIMENT_ID)).thenReturn(RULE);
        experimentRuleCacheUpdateEnvelope.run();
        // Confirm that the set rule is executed once with a null value
        Mockito.verify(ruleCache, Mockito.times(1)).setRule(EXPERIMENT_ID, null);
    }
}
