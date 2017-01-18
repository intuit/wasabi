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
package com.intuit.wasabi.analyticsobjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.intuit.wasabi.analyticsobjects.metrics.BinomialMetrics;
import com.intuit.wasabi.analyticsobjects.metrics.BinomialMetrics.BinomialMetric;
import com.intuit.wasabi.exceptions.AnalyticsException;
import com.intuit.wasabi.experimentobjects.Context;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Parameters available for use when calling analytics APIs.
 *
 * valid for both counts and statistics APIs:
 * fromTime, toTime: end points on time interval to consider
 * actions: subset of actions to consider
 *
 * valid for statistics APIs only:
 * metric: metric to use for statistics calculations
 * confidenceLevel: confidence level on (0, 1) scale, so 0.95 = 95%
 * effectSize: difference in action rates on [-1, 1] scale
 * singleShot: boolean flag indicating if the amount of data to be collected is predetermined and fixed
 *
 * not for public use:
 * mode: used for statistics testing
 */
@ApiModel(description = "Input parameters for the Analytics APIs.")
public class Parameters implements Cloneable {

    //specifiable parameters
    @ApiModelProperty(value = "ignore events prior to this time", example = "2014-06-10T00:00:00-0000")
    private Date fromTime = null;
    @ApiModelProperty(value = "ignore events after this time", example = "2014-06-10T00:00:00-0000")
    private Date toTime = null;
    @ApiModelProperty(value = "confidence level for statistics calculations; in range (0, 1)")
    private Double confidenceLevel = 0.95;
    @ApiModelProperty(value = "effect size of interest; in range [-1, 1]")
    private Double effectSize = 0.05;
    @ApiModelProperty(value = "only evaluate these actions")
    private List<String> actions = null;
    @JsonProperty("isSingleShot")
    @ApiModelProperty(value = "ask if you're interested, otherwise ignore")
    private Boolean singleShot = false;
    @ApiModelProperty(value = "ask if you're interested, otherwise ignore")
    private BinomialMetrics metric = BinomialMetrics.NORMAL_APPROX_SYM;
    @ApiModelProperty(value = "DO NOT USE")
    private Mode mode = Mode.PRODUCTION;
    @ApiModelProperty(value = "context of the experiment, eg \"QA\", \"PROD\"", dataType = "String")
    private Context context = Context.valueOf("PROD");

    //derived parameters
    /**
     * This field is set by calling the {@link #parse()} method that calculates the concrete instance.
     */
    @JsonIgnore
    private BinomialMetric metricImpl;
    @ApiModelProperty(value = "time zone")
    private TimeZone timeZone = null;

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public Date getFromTime() {
        return fromTime;
    }

    public void setFromTime(Date fromTime) {
        this.fromTime = fromTime;
    }

    public Date getToTime() {
        return toTime;
    }

    public void setToTime(Date toTime) {
        this.toTime = toTime;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context value) {
        this.context = value;
    }

    public List<String> getActions() {
        return actions;
    }

    public void setActions(List<String> acts) {
        this.actions = acts;
    }

    public BinomialMetrics getMetric() {
        return metric;
    }

    public void setMetric(BinomialMetrics met) {
        this.metric = met;
    }

    public Double getConfidenceLevel() {
        return confidenceLevel;
    }

    public void setConfidenceLevel(Double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
        // fixme: leverage javax.validation
        if (confidenceLevel <= 0.0 || confidenceLevel >= 1.0) {
            throw new IllegalArgumentException("Confidence level has to be between 0.0 and 1.0");
        }
    }

    public Double getEffectSize() {
        return effectSize;
    }

    public void setEffectSize(Double effectSize) {
        this.effectSize = effectSize;
        // fixme: leverage javax.validation
        if (effectSize < -1.0 || effectSize > 1.0) {
            throw new IllegalArgumentException("effect size level has to be between 0.0 and 1.0");
        }
    }

    public Boolean isSingleShot() {
        return singleShot;
    }

    public void setSingleShot(Boolean singleShot) {
        this.singleShot = singleShot;
    }

    public Mode getMode() {
        return mode;
    }

    public BinomialMetric getMetricImpl() {
        return metricImpl;
    }

    /**
     * Calculates the derived instance of a {@link BinomialMetric} from the other parameters.
     */
    public void parse() {
        if (singleShot) {
            metricImpl = metric.constructMetric(confidenceLevel, 1.0);
        } else {
            metricImpl = metric.constructMetric(confidenceLevel);
        }
    }

    @Override
    public Parameters clone() {
        try {
            return (Parameters) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new AnalyticsException("Parameters clone not supported: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        int result = fromTime != null ? fromTime.hashCode() : 0;
        result = 31 * result + (toTime != null ? toTime.hashCode() : 0);
        result = 31 * result + (confidenceLevel != null ? confidenceLevel.hashCode() : 0);
        result = 31 * result + (effectSize != null ? effectSize.hashCode() : 0);
        result = 31 * result + (actions != null ? actions.hashCode() : 0);
        result = 31 * result + (singleShot != null ? singleShot.hashCode() : 0);
        result = 31 * result + (metric != null ? metric.hashCode() : 0);
        result = 31 * result + (mode != null ? mode.hashCode() : 0);
        result = 31 * result + (context != null ? context.hashCode() : 0);
        result = 31 * result + (metricImpl != null ? metricImpl.hashCode() : 0);
        result = 31 * result + (timeZone != null ? timeZone.hashCode() : 0);
        return result;
    }

    public enum Mode {
        PRODUCTION,
        TEST
    }
}
