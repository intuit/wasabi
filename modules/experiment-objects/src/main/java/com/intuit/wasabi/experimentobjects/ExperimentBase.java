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
package com.intuit.wasabi.experimentobjects;

import java.util.Date;
import java.util.Set;

/**
 * This interface is a quick workaround for the problem that {@link Experiment}, {@link PrioritizedExperiment}
 * and {@link NewExperiment} do not have a common interface.
 * <p>
 * TODO in future the structure should be changed to an interface called Experiment, a common mother class called ExperimentBase and then children of that for New..,Page..,Whatever..
 */
public interface ExperimentBase {

    /**
     * The ID of the experiment.
     *
     * @return a {@link com.intuit.wasabi.experimentobjects.Experiment.ID}
     */
    Experiment.ID getID();

    /**
     * Description of the experiment.
     *
     * @return the description field of the experiment
     */
    String getDescription();

    /**
     * Rule of the experiment. Formulated in Hyrule syntax.
     *
     * @return the rule field of the experiment
     */
    String getRule();

    /**
     * The time when the experiment starts.
     *
     * @return the starttime field of the experiment
     */
    Date getStartTime();

    /**
     * The time when the experiment ends.
     *
     * @return the endtime field of the experiment
     */
    Date getEndTime();

    /**
     * The label of the experiment.
     *
     * @return an {@link com.intuit.wasabi.experimentobjects.Experiment.Label}
     */
    Experiment.Label getLabel();

    /**
     * Returns the current state of the experiment.
     *
     * @return see also {@link com.intuit.wasabi.experimentobjects.Experiment.State}
     */
    Experiment.State getState();

    /**
     * All experiments run within a specific {@link Application}.
     *
     * @return the name of this application
     */
    Application.Name getApplicationName();

    /**
     * Returns whether personalization is enabled or not.
     *
     * @return <code>true</code> when personalization is enabled
     */
    Boolean getIsPersonalizationEnabled();

    /**
     * Returns the set of tags associated with ths experiment.
     *
     * @return a sorted set of Strings for all labels stored with the experiment.
     */
    Set<String> getTags();

}
