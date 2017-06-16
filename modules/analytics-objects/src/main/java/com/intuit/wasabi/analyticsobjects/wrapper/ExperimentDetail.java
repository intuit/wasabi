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
package com.intuit.wasabi.analyticsobjects.wrapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * An ExperimentDetail is a wrapper class which holds additional information
 * apart from the basics of an Experiment and is used in the card view feature of the UI.
 */
public class ExperimentDetail {

    private Experiment.ID id;

    private Experiment.State state;

    private Experiment.Label label;

    private Application.Name applicationName;

    private Date modificationTime;

    private Date startTime;

    private Date endTime;

    private boolean isFavorite;

    private List<BucketDetail> buckets;

    private long totalNumberUsers;

    private String description;

    private Set<String> tags;


    /**
     * This class holds the details for the Buckets. This is especially interesting for
     * running experiments that already have assigned users and corresponding action rates.
     */
    public class BucketDetail {

        @JsonProperty("isControl")
        private boolean control;

        private Bucket.Label label;

        private Bucket.State state;

        private double allocationPercent = 0.0;

        private double actionRate = 0.0;

        private double lowerBound = 0.0;

        private double upperBound = 0.0;

        private long count = 0;

        // null for the following fields mean that it is not yet determinable if they are winners/losers
        // since the experiment is not running long enough
        private Boolean winnerSoFar = null;
        private Boolean loserSoFar = null;

        private String description;

        /**
         * Creates a BucketDetail with the basic information that are available for all buckets.
         *
         * @param label             the label of the bucket
         * @param control           flag whether this bucket is control
         * @param allocationPercent the allocation percentage for this bucket
         * @param description       the description of the bucket
         * @see Bucket for further information
         */
        public BucketDetail(Bucket.Label label, boolean control, double allocationPercent, Bucket.State state,
                            String description) {
            setLabel(label);
            setControl(control);
            setAllocationPercent(allocationPercent);
            setState(state);
            setDescription(description);
        }

        public boolean isControl() {
            return control;
        }

        public void setControl(boolean control) {
            this.control = control;
        }

        public Bucket.Label getLabel() {
            return label;
        }

        public void setLabel(Bucket.Label label) {
            if (label != null && !isEmpty(label.toString()))
                this.label = label;
            else throw new IllegalArgumentException("The label of a bucket can not be empty");
        }

        public double getAllocationPercent() {
            return allocationPercent;
        }

        public void setAllocationPercent(double allocationPercent) {
            if (allocationPercent >= 0.0 && allocationPercent <= 1.0)
                this.allocationPercent = allocationPercent;
            else throw new IllegalArgumentException("AllocationPercent must be between 0.0 and 1.0 for a bucket");
        }

        public double getActionRate() {
            return actionRate;
        }

        public void setActionRate(double actionRate) {
            if (!Double.isNaN(actionRate)) {
                this.actionRate = actionRate;
            }
        }

        public double getLowerBound() {
            return lowerBound;
        }

        public void setLowerBound(double lowerBound) {
            this.lowerBound = lowerBound;
        }

        public double getUpperBound() {
            return upperBound;
        }

        public void setUpperBound(double upperBound) {
            this.upperBound = upperBound;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            if (count >= 0)
                this.count = count;
            else throw new IllegalArgumentException("User count can not be smaller than 0");
        }

        public Boolean isWinnerSoFar() {
            return winnerSoFar;
        }

        public void setWinnerSoFar(Boolean winnerSoFar) {
            this.winnerSoFar = winnerSoFar;
        }

        public Boolean isLoserSoFar() {
            return loserSoFar;
        }

        public void setLoserSoFar(Boolean loserSoFar) {
            this.loserSoFar = loserSoFar;
        }

        public Bucket.State getState() {
            return state;
        }

        public void setState(Bucket.State state) {
            this.state = state;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * Creates the ExperimentDetail with a given Experiment and the total number of users.
     *
     * @param exp the experiment that provides the basic information
     */
    public ExperimentDetail(Experiment exp) {
        this(exp.getID(), exp.getState(), exp.getLabel(), exp.getApplicationName(),
                exp.getModificationTime(), exp.getStartTime(), exp.getEndTime(), exp.getDescription(),
                exp.getTags());
    }

    /**
     * Creates the ExperimentDetail with explicit values for the necessary fields.
     *
     * @param id               the id of the Experiment
     * @param state            the state of the Experiment
     * @param label            the Experiment label (name)
     * @param appName          the name of the Application this Experiment belongs to
     * @param modificationTime the last time the experiment was modified
     * @param startTime        the startTime of the experiment to determine the winner so far
     * @param endTime          the endtime of the experiment
     * @param description      the description of the experiment
     * @param tags             the tags of the experiment
     */
    public ExperimentDetail(Experiment.ID id, Experiment.State state, Experiment.Label label,
                            Application.Name appName, Date modificationTime, Date startTime,
                            Date endTime, String description, Set<String> tags) {
        setId(id);
        setState(state);
        setLabel(label);
        setApplicationName(appName);
        setModificationTime(modificationTime);
        setStartTime(startTime);
        setEndTime(endTime);
        setDescription(description);
        setTags(tags);
    }

    public Experiment.ID getId() {
        return id;
    }

    private void setId(Experiment.ID id) {
        if (id != null && !isEmpty(id.toString()))
            this.id = id;
        else
            throw new IllegalArgumentException("Can not create ExperimentDetail without an Experiment.ID");
    }

    public Experiment.State getState() {
        return state;
    }

    private void setState(Experiment.State state) {
        if (state != null && !Experiment.State.DELETED.equals(state))
            this.state = state;
        else
            throw new IllegalArgumentException("Experiment.State is not allowed to be null or DELETED for ExperimentDetail");
    }

    public Experiment.Label getLabel() {
        return label;
    }

    private void setLabel(Experiment.Label label) {
        if (label != null)
            this.label = label;
        else throw new IllegalArgumentException("Experiment.Label is not allowed to be null for ExperimentDetail");
    }

    public Application.Name getApplicationName() {
        return applicationName;
    }

    private void setApplicationName(Application.Name applicationName) {
        if (applicationName != null && !isEmpty(applicationName.toString()))
            this.applicationName = applicationName;
        else throw new IllegalArgumentException("Application Name can not be empty for ExperimentDetail");
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public List<BucketDetail> getBuckets() {
        return buckets;
    }

    private void setBuckets(List<BucketDetail> buckets) {
        this.buckets = buckets;
    }

    public long getTotalNumberUsers() {
        return totalNumberUsers;
    }

    public void setTotalNumberUsers(long totalNumberUsers) {
        if (totalNumberUsers > -1)
            this.totalNumberUsers = totalNumberUsers;
        else throw new IllegalArgumentException("Total number of users has to be equal or greater than zero");
    }

    public Date getModificationTime() {
        return modificationTime;
    }

    public void setModificationTime(Date modificationTime) {
        this.modificationTime = modificationTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    /**
     * This method takes a list of buckets and transforms it to the {@link BucketDetail}s that are needed
     * for later extension.
     *
     * @param buckets a list of {@link Bucket}s
     */
    public void addBuckets(List<Bucket> buckets) {

        List<BucketDetail> details = buckets.stream()
                .map(b -> new BucketDetail(b.getLabel(), b.isControl(), b.getAllocationPercent(), b.getState(),
                        b.getDescription()))
                .collect(Collectors.toList());

        setBuckets(details);

    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }


}
