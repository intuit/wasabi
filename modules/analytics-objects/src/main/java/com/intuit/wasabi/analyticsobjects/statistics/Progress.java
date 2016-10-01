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
package com.intuit.wasabi.analyticsobjects.statistics;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.intuit.wasabi.analyticsobjects.exceptions.AnalyticsException;
import com.intuit.wasabi.experimentobjects.Bucket;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO to save the progress of a bucket or an action. <br>
 *
 * Fields:
 * <ul>
 * <li>list of winners</li>
 * <li>list of losers</li>
 * <li>boolean to indicate if sufficient data has been collected</li>
 * <li>double to indicate the fraction of data collected so far</li>
 * </ul>
 */
public class Progress implements Cloneable {

    @ApiModelProperty(value = "list of winning buckets", required = true)
    //todo: this should really be a set
    protected List<Bucket.Label> winnersSoFar;
    @ApiModelProperty(value = "list of losing buckets", required = true)
    //todo: this should really be a set
    protected List<Bucket.Label> losersSoFar;
    @ApiModelProperty(value = "if sufficient data has been collected to observe the effect size of interest",
                      required = true)
    protected boolean hasSufficientData;
    @ApiModelProperty(value = "fraction of data that has been collected to observe the effect size of interest",
                      required = true)
    protected Double fractionDataCollected;

    public List<Bucket.Label> getWinnersSoFar() {
        return winnersSoFar;
    }

    public void setWinnersSoFar(List<Bucket.Label> value) {
        this.winnersSoFar = value;
    }

    public List<Bucket.Label> getLosersSoFar() {
        return losersSoFar;
    }

    public void setLosersSoFar(List<Bucket.Label> value) {
        this.losersSoFar = value;
    }

    public boolean isHasSufficientData() {
        return hasSufficientData;
    }

    public void setHasSufficientData(boolean value) {
        this.hasSufficientData = value;
    }

    public Double getFractionDataCollected() {
        return fractionDataCollected;
    }

    public void setFractionDataCollected(Double value) {
        this.fractionDataCollected = value;
    }

    @JsonIgnore
    public void addToWinnersSoFarList(Bucket.Label winner) {
        if (this.winnersSoFar == null) {
            this.winnersSoFar = new ArrayList<>();
        } else if (winner == null) {
            throw new IllegalArgumentException();
        } else {
            this.winnersSoFar.add(winner);
        }
    }

    @JsonIgnore
    public void addToLosersSoFarList(Bucket.Label loser) {
        if (this.losersSoFar == null) {
            this.losersSoFar = new ArrayList<>();
        } else if (loser == null) {
            throw new IllegalArgumentException();
        } else {
            this.losersSoFar.add(loser);
        }
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
    public Progress clone() {
        try {
            return (Progress) super.clone();
        } catch (CloneNotSupportedException e) {
            // Should never happen
            throw new AnalyticsException("Progress clone not supported: " + e.getMessage(), e);
        }
    }

    public static class Builder {

        private Progress item;

        public Builder() {
            this.item = new Progress();
        }

        public Builder withWinnersSoFar(List<Bucket.Label> value) {
            this.item.winnersSoFar = value;
            return this;
        }

        public Builder withLosersSoFar(List<Bucket.Label> value) {
            this.item.losersSoFar = value;
            return this;
        }

        public Builder withSufficientData(boolean value) {
            this.item.hasSufficientData = value;
            return this;
        }

        public Builder withFractionDataCollected(Double value) {
            this.item.fractionDataCollected = value;
            return this;
        }

        public Progress build() {
            return this.item;
        }
    }
}
