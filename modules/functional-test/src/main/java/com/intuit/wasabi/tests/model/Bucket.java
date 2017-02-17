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
package com.intuit.wasabi.tests.model;

import com.google.gson.FieldAttributes;
import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;

import java.lang.reflect.Field;
import java.util.Objects;

/**
 * A simplified Bucket class which can be instantiated in various ways and
 * modified directly via methods or public access to make it easy to test around with it.
 */
public class Bucket extends ModelItem {

    /**
     * The serialization strategy for comparisons and JSON serialization.
     */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();
    /**
     * The experiment Label. Required, unique among experiment.
     */
    public String label;
    /**
     * The experiment ID. Required.
     */
    public String experimentID;
    /**
     * The bucket's allocation percentage. Required.
     */
    public double allocationPercent;
    /**
     * The bucket's control group status. Required. Only one bucket per experiment must be the control group.
     */
    public boolean isControl;
    /**
     * The bucket's description. Optional.
     */
    public String description;
    /**
     * The bucket's payload. Optional.
     */
    public String payload;
    /**
     * The bucket's state. Optional.
     */
    public String state;

    /**
     * Creates a bucket without any members set.
     */
    public Bucket() {
    }

    /**
     * Creates a deep copy of the {@code other} Bucket.
     *
     * @param other a Bucket to copy.
     */
    public Bucket(Bucket other) {
        update(other);
    }

    /**
     * Creates a bucket with default values for all required arguments.
     * By default all buckets created are no control-buckets, so consider
     * using an appropriate constructor or setting the control-option for one
     * bucket.
     *
     * @param label             the bucket label (will be prefixed)
     * @param experiment        the experiment
     * @param allocationPercent the percentage that should go into this bucket
     */
    public Bucket(String label, Experiment experiment, double allocationPercent) {
        this(label, experiment.id, allocationPercent);
    }

    /**
     * Creates a bucket with default values for all required arguments.
     * By default all buckets created are no control-buckets, so consider
     * using an appropriate constructor or setting the control-option for one
     * bucket.
     *
     * @param label             the bucket label (will be prefixed)
     * @param experimentId      the experiment id
     * @param allocationPercent the percentage that should go into this bucket
     */
    public Bucket(String label, String experimentId, double allocationPercent) {
        this(label, experimentId, allocationPercent, false);
    }

    /**
     * Creates a bucket with default values for all required arguments.
     *
     * @param label             the bucket label (will be prefixed)
     * @param experiment        the experiment
     * @param allocationPercent the percentage that should go into this bucket
     * @param isControl         true if this bucket is the control group, else false
     */
    public Bucket(String label, Experiment experiment, double allocationPercent, boolean isControl) {
        this(label, experiment.id, allocationPercent, isControl, null);
    }

    /**
     * Creates a bucket with default values for all required arguments.
     *
     * @param label             the bucket label (will be prefixed)
     * @param experimentId      the experiment id
     * @param allocationPercent the percentage that should go into this bucket
     * @param isControl         true if this bucket is the control group, else false
     */
    public Bucket(String label, String experimentId, double allocationPercent, boolean isControl) {
        this(label, experimentId, allocationPercent, isControl, null);
    }

    /**
     * Creates a bucket with the supplied values.
     *
     * @param label             the bucket label (will be prefixed)
     * @param experiment        the experiment
     * @param allocationPercent the percentage that should go into this bucket
     * @param isControl         true if this bucket is the control group, else false
     * @param description       the bucket description
     */
    public Bucket(String label, Experiment experiment, double allocationPercent, boolean isControl, String description) {
        this(label, experiment.id, allocationPercent, isControl, description, null);
    }

    /**
     * Creates a bucket with the supplied values.
     *
     * @param label             the bucket label (will be prefixed)
     * @param experimentId      the experiment id
     * @param allocationPercent the percentage that should go into this bucket
     * @param isControl         true if this bucket is the control group, else false
     * @param description       the bucket description
     */
    public Bucket(String label, String experimentId, double allocationPercent, boolean isControl, String description) {
        this(label, experimentId, allocationPercent, isControl, description, null);
    }

    /**
     * Creates a bucket with the supplied values.
     *
     * @param label             the bucket label (will be prefixed)
     * @param experiment        the experiment
     * @param allocationPercent the percentage that should go into this bucket
     * @param isControl         true if this bucket is the control group, else false
     * @param description       the bucket description
     * @param payload           the payload for this bucket
     */
    public Bucket(String label, Experiment experiment, double allocationPercent, boolean isControl, String description,
                  String payload) {
        this(label, experiment.id, allocationPercent, isControl, description, payload, null);
    }

    /**
     * Creates a bucket with the supplied values.
     *
     * @param label             the bucket label (will be prefixed)
     * @param experimentId      the experiment id
     * @param allocationPercent the percentage that should go into this bucket
     * @param isControl         true if this bucket is the control group, else false
     * @param description       the bucket description
     * @param payload           the payload for this bucket
     */
    public Bucket(String label, String experimentId, double allocationPercent, boolean isControl, String description,
                  String payload) {
        this(label, experimentId, allocationPercent, isControl, description, payload, null);
    }

    /**
     * Creates a bucket with the supplied values.
     *
     * @param label             the bucket label (will be prefixed)
     * @param experimentID      the experimentID
     * @param allocationPercent the percentage that should go into this bucket
     * @param isControl         true if this bucket is the control group, else false
     * @param description       the bucket description
     * @param payload           the payload for this bucket
     * @param state             the desired state
     */
    public Bucket(String label, String experimentID, double allocationPercent, boolean isControl, String description,
                  String payload, String state) {
        this.setLabel(label)
                .setExperimentID(experimentID)
                .setAllocationPercent(allocationPercent)
                .setControl(isControl)
                .setDescription(description)
                .setPayload(payload)
                .setState(state);
    }

    /**
     * Sets the label to {@code label} and returns this instance.
     * Allows for builder patterns.
     *
     * @param label the label to set
     * @return this
     */
    public Bucket setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the experiment id and returns this instance.
     * Allows for builder patterns.
     *
     * @param experimentID the experiment id to be set
     * @return this
     */
    public Bucket setExperimentID(String experimentID) {
        this.experimentID = experimentID;
        return this;
    }

    /**
     * Sets the experiment id and returns this instance.
     * Allows for builder patterns.
     *
     * @param experiment the experiment to be set
     * @return this
     */
    public Bucket setExperimentID(Experiment experiment) {
        return this.setExperimentID(experiment.id);
    }

    /**
     * Alias for setExperimentId.
     * Sets the experiment id and returns this instance.
     * Allows for builder patterns.
     *
     * @param experiment the experiment to be set
     * @return this
     */
    public Bucket setExperiment(Experiment experiment) {
        return this.setExperimentID(experiment);
    }

    /**
     * Alias for setExperimentId.
     * Sets the experiment id and returns this instance.
     * Allows for builder patterns.
     *
     * @param experimentID the experiment id to be set
     * @return this
     */
    public Bucket setExperiment(String experimentID) {
        return this.setExperimentID(experimentID);
    }

    /**
     * Sets the allocation percentage {@code (0, 1]} and returns this instance.
     * Allows for builder patterns.
     *
     * @param allocationPercent the percentage to be set
     * @return this
     */
    public Bucket setAllocationPercent(double allocationPercent) {
        this.allocationPercent = allocationPercent;
        return this;
    }

    /**
     * Sets the control group status and returns this instance.
     * Allows for builder patterns.
     *
     * @param isControl true if this shall be the control group
     * @return this
     */
    public Bucket setControl(boolean isControl) {
        this.isControl = isControl;
        return this;
    }

    /**
     * Sets the description and returns this instance.
     * Allows for builder patterns.
     *
     * @param description the description to be set
     * @return this
     */
    public Bucket setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the payload and returns this instance.
     * Allows for builder patterns.
     *
     * @param payload the payload to be set
     * @return this
     */
    public Bucket setPayload(String payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Sets the state and returns this instance.
     * Allows for builder patterns.
     *
     * @param state the state to be set
     * @return this
     */
    public Bucket setState(String state) {
        this.state = state;
        return this;
    }

    @Override
    public SerializationStrategy getSerializationStrategy() {
        return Bucket.serializationStrategy;
    }

    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        Bucket.serializationStrategy = serializationStrategy;
    }

    /**
     * This is a workaround for the allocation percentage! If they are not equal, they are compared again
     * only taking the first 5 decimal digits into account, which should usually be more than enough.
     *
     * @param other another object
     * @return true if both are equal, with the exception of the allocation percentage as described above.
     */
    @Override
    public boolean equals(Object other) {
        if (super.equals(other)) return true;
        LOGGER.info("Retrying bucket comparison, maybe allocation percentages have floating point inaccuracies.");
        if (!this.getClass().isInstance(other)) {
            return false;
        }

        for (Field field : this.getClass().getFields()) {
            if (!this.getSerializationStrategy().shouldSkipField(new FieldAttributes(field))) {
                try {
                    boolean thisFieldEquals = Objects.equals(field.get(this), field.get(other));
                    if (!thisFieldEquals && field.getName().equals("allocationPercent")) {
                        LOGGER.debug("Retrying comparison of allocation percentages: " + this.allocationPercent
                                + " and " + ((Bucket) other).allocationPercent);
                        String allocThis = String.valueOf(this.allocationPercent);
                        String allocOther = String.valueOf(((Bucket) other).allocationPercent);
                        allocThis = allocThis.substring(0, Math.min(allocThis.indexOf(".") + 6, allocThis.length()));
                        allocOther = allocOther.substring(0, Math.min(allocOther.indexOf(".") + 6, allocOther.length()));
                        LOGGER.debug("\tComparing only " + allocThis + " and " + allocOther);
                        thisFieldEquals = Objects.equals(allocThis, allocOther);
                        LOGGER.debug("\tResult: " + (thisFieldEquals ? "" : "not ") + "equal.");
                    }
                    if (!thisFieldEquals) {
                        return false;
                    }
                } catch (IllegalAccessException ignored) {
                    // do nothing, just skip this field
                }
            }
        }
        return true;
    }

}

