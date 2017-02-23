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

import com.intuit.wasabi.tests.library.util.serialstrategies.DefaultNameExclusionStrategy;
import com.intuit.wasabi.tests.library.util.serialstrategies.SerializationStrategy;

/**
 * A very simple Assignment wrapper.
 */
public class Assignment extends ModelItem {
    /**
     * the assigned bucket
     */
    public String assignment;

    /**
     * this assignment's payload
     */
    public String payload;

    /**
     * the assignment status
     */
    public String status;

    /**
     * the assignment context
     */
    public String context;

    /**
     * the assignment cache value
     */
    public boolean cache;

    /**
     * the assignment's overwrite value
     */
    public boolean overwrite = true;

    /**
     * the experiment label for this assignment
     */
    public String experimentLabel;

    /**
     * creation date
     */
    public String created;

    /**
     * experiment ID
     */
    public String experiment_id;

    /**
     * user id
     */
    public String user_id;

    /**
     * Bucket label
     */
    public String bucket_label;

    /**
     * The serialization strategy for comparisons and JSON serialization.
     */
    private static SerializationStrategy serializationStrategy = new DefaultNameExclusionStrategy();

    /**
     * Creates an empty experiment.
     */
    public Assignment() {
    }

    /**
     * Creates a deep copy of the {@code other} experiment.
     *
     * @param other an experiment to copy.
     */
    public Assignment(Assignment other) {
        update(other);
    }

    /**
     * Sets the assignment bucket.
     *
     * @param assignment the new assignment
     * @return this
     */
    public Assignment setAssignment(String assignment) {
        this.assignment = assignment;
        return this;
    }

    /**
     * Sets the payload.
     *
     * @param payload the new payload
     * @return this
     */
    public Assignment setPayload(String payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Sets the status.
     *
     * @param status the new status
     * @return this
     */
    public Assignment setStatus(String status) {
        this.status = status;
        return this;
    }

    /**
     * Sets the context.
     *
     * @param context the new context
     * @return this
     */
    public Assignment setContext(String context) {
        this.context = context;
        return this;
    }

    /**
     * Sets the cache.
     *
     * @param cache the new cache
     * @return this
     */
    public Assignment setCache(boolean cache) {
        this.cache = cache;
        return this;
    }

    /**
     * Sets the overwrite.
     *
     * @param overwrite the new overwrite
     * @return this
     */
    public Assignment setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
        return this;
    }

    /**
     * Sets the experimentLabel.
     *
     * @param experimentLabel the new experimentLabel
     * @return this
     */
    public Assignment setExperimentLabel(String experimentLabel) {
        this.experimentLabel = experimentLabel;
        return this;
    }


    @Override
    public void setSerializationStrategy(SerializationStrategy serializationStrategy) {
        Assignment.serializationStrategy = serializationStrategy;
    }

    @Override
    public SerializationStrategy getSerializationStrategy() {
        return Assignment.serializationStrategy;
    }

}
