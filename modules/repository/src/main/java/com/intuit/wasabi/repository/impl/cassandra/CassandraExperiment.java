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

import com.google.common.base.Preconditions;
import com.intuit.hyrule.Rule;
import com.intuit.hyrule.RuleBuilder;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentIDSerializer;
import com.netflix.astyanax.model.ColumnList;

import java.util.Date;

/**
 * An immutable {@link Experiment} read from Cassandra
 */
/*pkg*/ class CassandraExperiment extends Experiment {

    /**
     * Create an instance
     *
     * @param columns The list of columns from the <code>Experiment</code> column
     *                family
     */
    public CassandraExperiment(ColumnList<String> columns) {
        super();

        super.setID(
                Preconditions.checkNotNull(
                        columns.getValue("id",
                                ExperimentIDSerializer.get(), null)));
        super.setDescription(
                columns.getStringValue("description", null));
        super.setRule(
                columns.getStringValue("rule", null));
        super.setRuleJson(convertRuleToJson(columns.getStringValue("rule", null)));
        super.setSamplingPercent(Preconditions.checkNotNull(
                columns.getDoubleValue("sample_percent", null)));
        super.setStartTime(Preconditions.checkNotNull(
                columns.getDateValue("start_time", null)));
        super.setEndTime(Preconditions.checkNotNull(
                columns.getDateValue("end_time", null)));
        super.setState(
                State.valueOf(
                        Preconditions.checkNotNull(
                                columns.getStringValue("state", null))));
        super.setLabel(
                Experiment.Label.valueOf(
                        Preconditions.checkNotNull(
                                columns.getStringValue("label", null))));
        super.setApplicationName(
                Application.Name.valueOf(
                        Preconditions.checkNotNull(
                                columns.getStringValue("app_name", null))));
        super.setCreationTime(Preconditions.checkNotNull(
                columns.getDateValue("created", null)));
        super.setModificationTime(Preconditions.checkNotNull(
                columns.getDateValue("modified", null)));
        super.setIsPersonalizationEnabled(Preconditions.
                checkNotNull(columns.getBooleanValue("is_personalized", false)));
        super.setModelName(Preconditions.checkNotNull(columns.getStringValue("model_name", "")));
        super.setModelVersion(Preconditions.checkNotNull(columns.getStringValue("model_version", "")));
        super.setIsRapidExperiment(Preconditions.checkNotNull(columns.getBooleanValue("is_rapid_experiment", false)));
        super.setUserCap(Preconditions.checkNotNull(columns.getIntegerValue("user_cap", Integer.MAX_VALUE)));
        super.setCreatorID(columns.getStringValue("creatorid", null));
    }

    private String convertRuleToJson(String rule) {
        String decoratedRule = "";
        if (rule != null && !rule.isEmpty()) {
            Rule expRule = new RuleBuilder().parseExpression(rule);
            decoratedRule = expRule.getJSONRepresentation();
        }
        return decoratedRule;
    }

    @Override
    public void setID(ID id) {
        throwNotMutableException();
    }

    @Override
    public void setDescription(String description) {
        throwNotMutableException();
    }

    @Override
    public void setRule(String rule) {
        throwNotMutableException();
    }

    @Override
    public void setSamplingPercent(Double samplingPercent) {
        throwNotMutableException();
    }

    @Override
    public void setStartTime(Date startTime) {
        throwNotMutableException();
    }

    @Override
    public void setEndTime(Date endTime) {
        throwNotMutableException();
    }

    @Override
    public void setState(State state) {
        throwNotMutableException();
    }

    @Override
    public void setLabel(Experiment.Label value) {
        throwNotMutableException();
    }

    @Override
    public void setApplicationName(Application.Name appName) {
        throwNotMutableException();
    }

    @Override
    public void setCreationTime(Date creationTime) {
        throwNotMutableException();
    }

    @Override
    public void setModificationTime(Date modificationTime) {
        throwNotMutableException();
    }

    @Override
    public void setIsPersonalizationEnabled(Boolean isPersonalizationEnabled) {
        throwNotMutableException();
    }

    @Override
    public void setModelName(String modelName) {
        throwNotMutableException();
    }

    @Override
    public void setModelVersion(String modelVersion) {
        throwNotMutableException();
    }

    @Override
    public void setIsRapidExperiment(Boolean isRapidExperiment) {
        throwNotMutableException();
    }

    @Override
    public void setUserCap(Integer userCap) {
        throwNotMutableException();
    }

    @Override
    public void setCreatorID(String creatorID) {
        throwNotMutableException();
    }

    /**
     * Signals that this instance cannot be modified. This is a HACK because
     * the base type should be an immutable interface rather than a bean.
     */
    private void throwNotMutableException() {
        throw new UnsupportedOperationException("This instance is not mutable");
    }
}
