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
package com.intuit.wasabi.eventlog.events;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.ExperimentBase;

/**
 * An event to denote that a bucket was deleted.
 */
public class BucketDeleteEvent extends AbstractEvent implements BucketEvent {

    private final ExperimentBase experiment;
    private final Bucket bucket;
    private final Application.Name appName;

    /**
     * Creates a BucketDeleteEvent by the system user.
     *
     * @param experiment the experiment
     * @param bucket the deleted bucket
     */
    public BucketDeleteEvent(ExperimentBase experiment, Bucket bucket) {
        this(null, experiment, bucket);
    }

    /**
     * Creates a BucketDeleteEvent.
     *
     * @param user the user (if null, the {@link EventLog#SYSTEM_USER} is used)
     * @param experiment the experiment
     * @param bucket the deleted bucket
     */
    public BucketDeleteEvent(UserInfo user, ExperimentBase experiment, Bucket bucket) {
        super(user);
        this.experiment = experiment;
        this.bucket = bucket;
        this.appName = experiment.getApplicationName();
    }

    /**
     * The affected bucket.
     *
     * @return the bucket
     */
    @Override
    public Bucket getBucket() {
        return bucket;
    }

    /**
     * The affected experiment.
     *
     * @return the experiment.
     */
    @Override
    public ExperimentBase getExperiment() {
        return experiment;
    }

    /**
     * A default event description for non-specified event handlers.
     *
     * @return a simplified description
     */
    @Override
    public String getDefaultDescription() {
        return getUser().getUsername() + " deleted bucket " + bucket + " from experiment " + getExperiment();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Application.Name getApplicationName() {
        return appName;
    }
}
