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
import com.intuit.wasabi.experimentobjects.ExperimentBase;

/**
 * This event denotes a change in an experiment property.
 */
public class ExperimentChangeEvent extends AbstractChangeEvent implements ExperimentEvent {

    private final ExperimentBase experiment;
    private final Application.Name appName;

    /**
     * Creates an event denoting an experiment property change, invoked by the {@link EventLog#SYSTEM_USER}.
     *
     * @param experiment the experiment (must not be null)
     * @param propertyName the changed property (must not be blank)
     * @param before the state before
     * @param after the state after
     */
    public ExperimentChangeEvent(ExperimentBase experiment, String propertyName, String before, String after) {
        this(null, experiment, propertyName, before, after);
    }

    /**
     * Creates an event denoting an experiment property change.
     *
     * @param user the user
     * @param experiment the experiment (must not be null)
     * @param propertyName the changed property (must not be blank)
     * @param before the state before
     * @param after the state after
     */
    public ExperimentChangeEvent(UserInfo user, ExperimentBase experiment, String propertyName, String before, String after) {
        super(user, propertyName, before, after);
        if (experiment == null) {
            throw new IllegalArgumentException("Experiment must not be null!");
        }
        this.experiment = experiment;
        this.appName = experiment.getApplicationName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExperimentBase getExperiment() {
        return experiment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultDescription() {
        return getUser().getUsername() + " changed property " + getPropertyName() + " of experiment " + getExperiment().getLabel() + " from " + getBefore() + " to " + getAfter() + ".";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Application.Name getApplicationName() {
        return appName;
    }
}
