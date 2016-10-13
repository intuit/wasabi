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
package com.intuit.wasabi.repository.impl.cassandra;

import com.intuit.hyrule.Rule;
import com.intuit.hyrule.RuleBuilder;
import com.intuit.wasabi.assignmentobjects.RuleCache;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.slf4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Runnable for updating rule caching
 */
public class ExperimentRuleCacheUpdateEnvelope implements Runnable {

    private static final Logger LOGGER = getLogger(ExperimentRuleCacheUpdateEnvelope.class);
    private final SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, yyyy hh:mm:ss a z");
    private String cassandraRuleString;
    private RuleCache ruleCache;
    private Experiment.ID experimentID;

    public ExperimentRuleCacheUpdateEnvelope(String cassandraRuleString, RuleCache ruleCache, Experiment.ID experimentID) {
        this.cassandraRuleString = cassandraRuleString;
        this.ruleCache = ruleCache;
        this.experimentID = experimentID;
    }

    @Override
    public void run() {

        try {
            if (Objects.nonNull(cassandraRuleString)) {
                Rule cassandraRule = getExperimentRule(cassandraRuleString);
                if (!ruleCache.containsRule(experimentID) ||
                        !cassandraRule.equals(ruleCache.getRule(experimentID))) {
                    Rule oldRule = ruleCache.getRule(experimentID);
                    ruleCache.setRule(experimentID, cassandraRule);
                    LOGGER.info(getUTCTime() + " Segmentation rule of " + experimentID + " updated from " +
                            (Objects.nonNull(oldRule) ? oldRule.getExpressionRepresentation() : null) + " to " +
                            cassandraRule.getExpressionRepresentation());
                }
            } else if (Objects.nonNull(ruleCache.getRule(experimentID))) {
                Rule oldRule = ruleCache.getRule(experimentID);
                ruleCache.setRule(experimentID, null);
                LOGGER.info(getUTCTime() + " Segmentation rule of " + experimentID + " updated from " +
                        oldRule.getExpressionRepresentation() + " to null");
            }
        } catch (Exception e) {
            LOGGER.warn("RuleCache update error: " + e);
        }

    }

    protected String getUTCTime() {
        Date currentTime = new Date();
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(currentTime);
    }

    protected Rule getExperimentRule(String ruleString) {
        return new RuleBuilder().parseExpression(ruleString);
    }
}


