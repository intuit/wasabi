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
package com.intuit.wasabi.assignmentobjects;

import com.intuit.hyrule.Rule;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class RuleCacheTest {

    private Experiment.ID experimentID = Experiment.ID.newInstance();
    @Mock
    private Rule rule;
    private RuleCache ruleCache;

    @Before
    public void setUp() throws Exception {
        ruleCache = new RuleCache();
    }

    @Test
    public void testRuleCache() {
        ruleCache.setRule(experimentID, rule);
        assertTrue(ruleCache.containsRule(experimentID));
        assertNotNull(ruleCache.getRule(experimentID));

        ruleCache.clearRule(experimentID);
        assertTrue(!ruleCache.containsRule(experimentID));
    }

}
