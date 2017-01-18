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
package com.intuit.wasabi.analytics;

import com.intuit.wasabi.analyticsobjects.Parameters;
import com.intuit.wasabi.analyticsobjects.wrapper.ExperimentDetail;

import java.util.List;


/**
 * Allows for the retrieval of detailed information to an experiment, which includes besides the
 * basic attributes bucket information and analytic results.
 */
public interface ExperimentDetails {

    /**
     * Queries the database to generate a list of experiments. Those Experiments already have the bucket information
     * set, but will not have the analytic numbers for running experiments. The list does also not include experiments
     * with the state deleted.
     *
     * @return a list of ExperimentDetails containing basic information of the experiment and buckets
     */
    List<ExperimentDetail> getExperimentDetailsBase();

    /**
     * This method uses the Analytics module to retrieve detailed information of the current state of the Experiments
     * referenced in the details-list. Information included is: the number of assigned users, the action and error
     * rate per bucket etc.
     *
     * @param details a list of ExperimentDetails as it can be retrieved with {@link #getExperimentDetailsBase()}
     * @param params  the parameters necessary for the analytics calls {@link Parameters}
     * @return a completed list of ExperimentDetail
     */
    List<ExperimentDetail> getAnalyticData(List<ExperimentDetail> details, Parameters params);

}
