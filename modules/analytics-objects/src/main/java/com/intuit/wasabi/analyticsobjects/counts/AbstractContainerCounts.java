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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.intuit.wasabi.analyticsobjects.Event;
import com.intuit.wasabi.analyticsobjects.Event.Name;
import com.intuit.wasabi.exceptions.AnalyticsException;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The base class for containers for different counts that can be in an ExperimentCounts or a BucketCounts object
 */
public abstract class AbstractContainerCounts implements ContainerCounts, Cloneable {

    @ApiModelProperty(value = "counts for each action individually", required = true)
    protected Map<Event.Name, ActionCounts> actionCounts;
    @ApiModelProperty(value = "counts for impressions", required = true)
    protected Counts impressionCounts;
    @ApiModelProperty(value = "counts for all actions treated as one", required = true)
    protected Counts jointActionCounts;

    @Override
    public Map<Event.Name, ActionCounts> getActionCounts() {
        return actionCounts;
    }

    public void setActionCounts(Map<Event.Name, ActionCounts> value) {
        this.actionCounts = value;
    }

    @Override
    public Counts getImpressionCounts() {
        return impressionCounts;
    }

    public void setImpressionCounts(Counts value) {
        this.impressionCounts = value;
    }

    @Override
    public Counts getJointActionCounts() {
        return jointActionCounts;
    }

    public void setJointActionCounts(Counts value) {
        this.jointActionCounts = value;
    }

    @JsonIgnore
    public void addActionCounts(Event.Name actionName, ActionCounts actionCounts) {
        if (this.actionCounts == null) {
            this.actionCounts = new HashMap<>();
        }

        this.actionCounts.put(actionName, actionCounts);
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
    public AbstractContainerCounts clone() {
        AbstractContainerCounts cloned;

        try {
            cloned = (AbstractContainerCounts) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new AnalyticsException("AbstractContainerCounts clone not supported: " + e.getMessage(), e);
        }

        if (impressionCounts != null) {
            cloned.setImpressionCounts(impressionCounts.clone());
        }

        if (jointActionCounts != null) {
            cloned.setJointActionCounts(jointActionCounts.clone());
        }

        if (actionCounts != null) {
            Map<Event.Name, ActionCounts> clonedActions = new HashMap<>();
            for (Entry<Name, ActionCounts> entry : actionCounts.entrySet()) {
                clonedActions.put(entry.getKey(), entry.getValue().clone());
            }
            cloned.setActionCounts(clonedActions);
        }

        return cloned;
    }
}
