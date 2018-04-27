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

import com.google.common.base.Preconditions;
import com.intuit.hyrule.Rule;
import com.intuit.hyrule.RuleBuilder;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.Experiment.State;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Helper class for converting experiment related objects
 * TODO: Currently it has static methods but can be changed to non static instance which
 * can be injected
 */
public class ExperimentHelper {

    public static Experiment.ID makeExperimentId(UUID experimentId) {
        return Experiment.ID.valueOf(experimentId);
    }

    public static List<UUID> makeUUIDs(
            Collection<Experiment.ID> experimentIDCollection) {
        List<UUID> experimentIds = new ArrayList<>();
        for (ID id : experimentIDCollection) {
            experimentIds.add(id.getRawID());
        }
        return experimentIds;
    }

    public static Experiment makeExperiment(
            com.intuit.wasabi.repository.cassandra.pojo.Experiment experimentPojo) {

        Experiment experimentObject = Experiment
                .withID(makeExperimentId(experimentPojo.getId()))
                .withDescription(experimentPojo.getDescription())
                .withRule(experimentPojo.getRule())
                .withSamplingPercent(experimentPojo.getSamplePercent())
                .withStartTime(Preconditions.checkNotNull(experimentPojo.getStartTime()))
                .withCreationTime(Preconditions.checkNotNull(experimentPojo.getCreated()))
                .withEndTime(Preconditions.checkNotNull(experimentPojo.getEndTime()))
                .withState(
                        State.valueOf(
                                Preconditions.checkNotNull(experimentPojo.getState()))
                ).build();

        experimentObject.setLabel(Experiment.Label.valueOf(
                Preconditions.checkNotNull(experimentPojo.getLabel())));
        experimentObject.setApplicationName(
                Application.Name.valueOf(
                        Preconditions.checkNotNull(
                                experimentPojo.getAppName())));
        experimentObject.setModificationTime(Preconditions.checkNotNull(
                experimentPojo.getModified()));
        experimentObject.setSamplingPercent(Preconditions.checkNotNull(
                experimentPojo.getSamplePercent()));
        experimentObject.setIsPersonalizationEnabled(experimentPojo.isPersonalized());
        experimentObject.setModelName(experimentPojo.getModelName());
        experimentObject.setModelVersion(experimentPojo.getModelVersion());
        experimentObject.setIsRapidExperiment(experimentPojo.isRapidExperiment());
        experimentObject.setUserCap(experimentPojo.getUserCap());
        experimentObject.setCreatorID(experimentPojo.getCreatorId());
        experimentObject.setRuleJson(convertRuleToJson(experimentPojo.getRule()));
        experimentObject.setHypothesisIsCorrect(experimentPojo.getHypothesisIsCorrect());
        experimentObject.setResults(experimentPojo.getResults());
        experimentObject.setTags(experimentPojo.getTags());
        experimentObject.setSourceURL(experimentPojo.getSourceURL());
        experimentObject.setExperimentType(experimentPojo.getExperimentType());

        return experimentObject;
    }

    protected static String convertRuleToJson(String rule) {
        String decoratedRule = "";
        if (rule != null && !rule.isEmpty()) {
            Rule expRule = new RuleBuilder().parseExpression(rule);
            decoratedRule = expRule.getJSONRepresentation();
        }
        return decoratedRule;
    }

}
