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
package com.intuit.wasabi.analyticsobjects.statistics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.Event.Name;
import com.intuit.wasabi.analyticsobjects.counts.AbstractContainerCounts;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Base class for holding statistics and counts
 */
public abstract class AbstractContainerStatistics extends AbstractContainerCounts implements ContainerStatistics {

    @ApiModelProperty(value = "statistics for all actions treated as one", required = true)
    protected Estimate jointActionRate;
    @ApiModelProperty(value = "statistics for each action individually", required = true)
    protected Map<Event.Name, ActionRate> actionRates;

    @Override
    public Estimate getJointActionRate() {
        return jointActionRate;
    }

    public void setJointActionRate(Estimate value) {
        this.jointActionRate = value;
    }

    @Override
    public Map<Event.Name, ActionRate> getActionRates() {
        return actionRates;
    }

    public void setActionRates(Map<Event.Name, ActionRate> value) {
        this.actionRates = value;
    }

    @JsonIgnore
    public void addToActionRate(Event.Name actionName, ActionRate actionRate) {
        if (this.actionRates == null) {
            this.actionRates = new HashMap<>();
        }
        this.actionRates.put(actionName, actionRate);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public AbstractContainerStatistics clone() {
        AbstractContainerStatistics cloned = (AbstractContainerStatistics) super.clone();

        if (jointActionRate != null) {
            cloned.setJointActionRate(jointActionRate.clone());

        }

        if (actionRates != null) {
            Map<Event.Name, ActionRate> clonedActions = new HashMap<>();
            for (Entry<Name, ActionRate> entry : actionRates.entrySet()) {
                clonedActions.put(entry.getKey(), entry.getValue().clone());
            }
            cloned.setActionRates(clonedActions);
        }

        return cloned;
    }
}
