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
package com.intuit.wasabi.analyticsobjects.counts;

import com.intuit.wasabi.analyticsobjects.Event;

import java.util.Map;

/**
 * Interface for the various counts
 */
public interface ContainerCounts {

    /**
     * Gives the counts for Actions, either on a Bucket or Experiment level.
     *
     * @return a {@link Map} from the {@link com.intuit.wasabi.analyticsobjects.Event.Name} to the concrete {@link ActionCounts}
     */
    Map<Event.Name, ActionCounts> getActionCounts();

    /**
     * Gives the counted Impressions for an experiment or bucket.
     *
     * @return {@link Counts} for the Impressions
     */
    Counts getImpressionCounts();

    /**
     * Gives the action counts across an experiment.
     *
     * @return {@link Counts} for the JointActions
     */
    Counts getJointActionCounts();
}
