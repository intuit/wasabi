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
package com.intuit.wasabi.api;

import com.google.inject.Inject;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authorization.Authorization;
import com.intuit.wasabi.experiment.Experiments;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.exception.ApplicationNotFoundException;
import com.intuit.wasabi.experimentobjects.exception.ExperimentNotFoundException;

import java.util.List;
import java.util.Objects;

import static com.intuit.wasabi.authorizationobjects.Permission.READ;

class AuthorizedExperimentGetter {

    private final Authorization authorization;
    private final Experiments experiments;

    @Inject
    AuthorizedExperimentGetter(final Authorization authorization, final Experiments experiments) {
        this.authorization = authorization;
        this.experiments = experiments;
    }

    Experiment getAuthorizedExperimentById(final String authorizationHeader,
                                           final Experiment.ID experimentId) {
        UserInfo.Username userName = authorization.getUser(authorizationHeader);
        Experiment experiment = experiments.getExperiment(experimentId);

        if (Objects.isNull(experiment)) {
            throw new ExperimentNotFoundException(experimentId);
        }

        authorization.checkUserPermissions(userName, experiment.getApplicationName(), READ);

        return experiment;
    }

    Experiment getAuthorizedExperimentByName(final String authorizationHeader,
                                             final Application.Name applicationName,
                                             final Experiment.Label experimentLabel) {
        authorization.checkUserPermissions(authorization.getUser(authorizationHeader), applicationName, READ);

        Experiment experiment = experiments.getExperiment(applicationName, experimentLabel);

        if (Objects.isNull(experiment)) {
            throw new ExperimentNotFoundException(experimentLabel);
        }

        return experiment;
    }

    List<Experiment> getAuthorizedExperimentsByName(final String authorizationHeader,
                                                    final Application.Name applicationName) {
        authorization.checkUserPermissions(authorization.getUser(authorizationHeader), applicationName, READ);

        List<Experiment> experimentList = this.experiments.getExperiments(applicationName);

        if (Objects.isNull(experimentList)) {
            throw new ApplicationNotFoundException(applicationName);
        }

        return experimentList;
    }
}
