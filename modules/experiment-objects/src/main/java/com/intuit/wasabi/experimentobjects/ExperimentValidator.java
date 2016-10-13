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
package com.intuit.wasabi.experimentobjects;

import com.intuit.hyrule.RuleBuilder;
import com.intuit.hyrule.exceptions.InvalidSchemaException;
import com.intuit.hyrule.exceptions.InvalidSyntaxException;
import com.intuit.wasabi.experimentobjects.exception.InvalidBucketStateTransitionException;
import com.intuit.wasabi.experimentobjects.exception.InvalidExperimentStateException;
import com.intuit.wasabi.experimentobjects.exception.InvalidExperimentStateTransitionException;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Validates various model instances
 */
public class ExperimentValidator {

    public ExperimentValidator() {
        super();
    }

    public void validateExperiment(Experiment experiment) {


        Experiment.State state = experiment.getState();
        if (state.equals(Experiment.State.DELETED) ||
                state.equals(Experiment.State.TERMINATED)) {
            return;
        }

        validateExperimentStartEnd(experiment.getStartTime(), experiment.getEndTime());
        validateSamplingPercent(experiment.getSamplingPercent());
        validateExperimentRule(experiment.getRule());
        validateModelNameNotNullForPersonalizedExperiments(experiment.getIsPersonalizationEnabled()
                , experiment.getModelName());
    }

    public void validateNewExperiment(NewExperiment newExperiment) {

        validateExperimentStartEnd(newExperiment.getStartTime(), newExperiment.getEndTime());
        validateSamplingPercent(newExperiment.getSamplingPercent());
        validateExperimentRule(newExperiment.getRule());
        validateModelNameNotNullForPersonalizedExperiments(newExperiment.getIsPersonalizationEnabled(),
                newExperiment.getModelName());
        validateDescriptionNotEmpty(newExperiment.getDescription());
    }

    protected void validateModelNameNotNullForPersonalizedExperiments(Boolean isPersonalizationEnabled, String modelName) {
        if (Objects.nonNull(isPersonalizationEnabled) && isPersonalizationEnabled &&
                (Objects.isNull(modelName) || "".equals(modelName.trim()))) {
            throw new IllegalArgumentException("Personalization enabled without specification of a model name");
        }
    }

    protected void validateExperimentStartEnd(Date startTime, Date endTime) {
        if (Objects.nonNull((startTime)) && (Objects.nonNull(endTime)) && endTime.before(startTime)) {
            throw new IllegalArgumentException("Invalid date range, start = \"" + startTime + "\", end = " +
                    "\"" + endTime + "\"");
        }
    }

    protected void validateExperimentRule(String rule) {
        // validate if rule is not empty string
        if (Objects.nonNull(rule) && !rule.trim().isEmpty()) {
            try {
                new RuleBuilder().parseExpression(rule);
            } catch (InvalidSyntaxException | IllegalArgumentException | InvalidSchemaException e) {
                throw new IllegalArgumentException("Invalid rule.", e);
            }
        }
    }

    protected void validateSamplingPercent(Double rate) {
        if (Objects.isNull(rate)) {
            throw new IllegalArgumentException(
                    "Experiment sampling percent cannot be null");
        }

        if ((rate <= 0.0) || (rate > 1.0)) {
            throw new IllegalArgumentException("Sampling percent must be between 0.0 and 1.0 inclusive");
        }
    }

    /**
     * Validates potential experiment state changes.
     *
     * @param oldState the old experiment state
     * @param newState the new experiment state
     */
    public void validateStateTransition(Experiment.State oldState, Experiment.State newState) {

        if (!oldState.isStateTransitionAllowed(newState)) {
            throw new InvalidExperimentStateTransitionException("Invalid switch from state \"" + oldState +
                    "\" to invalid state \"" + newState + "\"");
        }
    }

    public void validateExperimentBuckets(List<Bucket> buckets) {

        if (Objects.isNull((buckets)) || (buckets.isEmpty())) {
            throw new IllegalArgumentException("No experiment buckets specified");
        }

        Double totalAllocation = 0.0;
        Integer numControl = 0;
        Integer nOpen = 0;
        for (Bucket bucket : buckets) {

            Double val = bucket.getAllocationPercent();

            totalAllocation += val;

            if (bucket.getState() == Bucket.State.OPEN || Objects.isNull(bucket.getState())) {
                nOpen++;
            }
            if (bucket.isControl()) {
                numControl++;
            }
        }

        if ((nOpen > 0 && Math.abs(totalAllocation - 1.0) > 1e-12) ||
                (nOpen == 0 && Double.doubleToRawLongBits(totalAllocation) != 0)) {
            throw new IllegalArgumentException("Total allocation must be 1.0 (or 0.0 if all buckets are closed/empty");
        }
        if (numControl > 1) {
            throw new IllegalArgumentException("Only one bucket may be specified as a control bucket");
        }
    }

    /**
     * Checks that an experiment has a state "draft" and throws an exception
     * if it doesn't
     *
     * @param experiment Experiment object containing experiment metadata
     */
    public void ensureStateIsDraft(Experiment experiment) {
        Experiment.State state = experiment.getState();

        if (!state.equals(Experiment.State.DRAFT)) {
            throw new InvalidExperimentStateException(experiment.getID(), Experiment.State.DRAFT, experiment.getState());
        }
    }

    public void validateBucketStateTransition(Bucket.State oldState, Bucket.State desiredState) {

        Bucket bucket = new Bucket();
        bucket.setState(oldState);

        if (!bucket.isStateTransitionValid(desiredState)) {
            throw new InvalidBucketStateTransitionException("Invalid switch from state \"" + oldState +
                    "\" to invalid state \"" + desiredState + "\"");
        }
    }

    /**
     * Throws an exception if description is {@link StringUtils#isEmpty(CharSequence)}.
     *
     * @param description the description to test.
     * @throws IllegalArgumentException on empty description
     */
    public void validateDescriptionNotEmpty(String description) {
        if (StringUtils.isEmpty(description)) {
            throw new IllegalArgumentException("Description/Hypothesis must not be empty.");
        }
    }
}
