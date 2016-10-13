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
package com.intuit.wasabi.eventlog.events;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.ExperimentBase;

import java.util.Objects;

/**
 * An event to be used when an experiment is created.
 */
public class ExperimentCreateEvent extends AbstractEvent implements ExperimentEvent, CreateEvent {

    private final ExperimentBase experiment;
    private final Application.Name appName;

    /**
     * Creates a new event denoting experiment creation, invoked by the {@link EventLog#SYSTEM_USER}.
     *
     * @param experiment the experiment (must not be null)
     */
    public ExperimentCreateEvent(ExperimentBase experiment) {
        this(null, experiment);
    }

    /**
     * Creates a new event denoting experiment creation.
     *
     * @param user       the user
     * @param experiment the experiment (must not be null)
     */
    public ExperimentCreateEvent(UserInfo user, ExperimentBase experiment) {
        super(user);
        if (Objects.isNull(experiment)) {
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
        return getUser().getUsername() + " created experiment " + getExperiment().getLabel() + ".";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Application.Name getApplicationName() {
        return appName;
    }
}
