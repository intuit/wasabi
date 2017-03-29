package com.intuit.wasabi.experimentobjects;

import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ajain11 on 3/29/17.
 */
public class ExperimentCountsByState {
    @ApiModelProperty(required = true)
    private Map<String, Integer> experimentCountsByState = new HashMap<>();

    public ExperimentCountsByState() {
        super();
    }

    public ExperimentCountsByState(final Map<String, Integer> experimentCountsByState) {
        super();
        this.experimentCountsByState = experimentCountsByState;
    }

    public Map<String, Integer> getExperimentCountsByState() {
        return experimentCountsByState;
    }

    public void setExperimentCountsByState(final Map<String, Integer> experimentCountsByState) {
        this.experimentCountsByState = experimentCountsByState;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }
}
