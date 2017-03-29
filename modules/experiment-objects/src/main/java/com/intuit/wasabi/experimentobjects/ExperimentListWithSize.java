package com.intuit.wasabi.experimentobjects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by ajain11 on 3/29/17.
 */
public class ExperimentListWithSize {

    private Integer size;

    private List<Experiment> experiments;

    public ExperimentListWithSize() {
        super();
        this.experiments = new LinkedList<>();
        size = 0;
    }

    public ExperimentListWithSize(final List<Experiment> experiments) {
        super();
        this.experiments = experiments;
        setSize();
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<Experiment> experiments) {
        this.experiments = experiments;
        setSize();
    }

    public void addExperimentToList(Experiment experiment) {
        experiments.add(experiment);
        size +=1;
    }

    public Integer getSize() {
        setSize();
        return size;
    }

    private void setSize(){
        this.size = experiments != null ? experiments.size(): 0;
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
