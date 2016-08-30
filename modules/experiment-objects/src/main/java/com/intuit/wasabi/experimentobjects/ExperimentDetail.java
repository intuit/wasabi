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
package com.intuit.wasabi.experimentobjects;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An ExperimentDetail is a wrapper class which holds additional information
 * apart from the basics of an Experiment and is used in the card view feature of the UI.
 */
public class ExperimentDetail {

    private Experiment.ID id;

    private Experiment.State state;

    private Experiment.Label label;

    private Application.Name appName;

    private boolean isFavorite;

    private List<BucketDetail> buckets;

    private long totalNumberUsers;


    /**
     * This class holds the details for the Buckets. This is especially interesting for
     * running experiments that already have assigned Users and corresponding action rates.
     */
    public class BucketDetail{

        private boolean isControl;

        private Bucket.Label label;

        private double allocationPercent = 0.0;

        private double actionRate = 0.0;

        private double errorRate = 0.0;

        private int userCount = 0;

        public BucketDetail(Bucket.Label label, boolean isControl, double allocationPercent){
            setLabel(label);
            setControl(isControl);
            setAllocationPercent(allocationPercent);
        }

        public boolean isControl() {
            return isControl;
        }

        public void setControl(boolean control) {
            isControl = control;
        }

        public Bucket.Label getLabel() {
            return label;
        }

        public void setLabel(Bucket.Label label) {
            if(label != null && !isEmpty(label.toString()))
                this.label = label;
            else throw new IllegalArgumentException("The label of a bucket can not be empty");
        }

        public double getAllocationPercent() {
            return allocationPercent;
        }

        public void setAllocationPercent(double allocationPercent) {
            if(allocationPercent >= 0.0)
                this.allocationPercent = allocationPercent;
            else throw new IllegalArgumentException("AllocationPercent can not be smaller than 0 for a bucket");
        }

        public double getActionRate() {
            return actionRate;
        }

        public void setActionRate(double actionRate) {
            this.actionRate = actionRate;
        }

        public double getErrorRate() {
            return errorRate;
        }

        public void setErrorRate(double errorRate) {
            this.errorRate = errorRate;
        }

        public int getUserCount() {
            return userCount;
        }

        public void setUserCount(int userCount) {
            if(userCount >= 0)
                this.userCount = userCount;
            else throw new IllegalArgumentException("User count can not be smaller than 0");
        }
    }

    /**
     * Creates the ExperimentDetail with a given Experiment and the total number of users.
     *
     * @param exp the experiment that provides the basic information
     */
    public ExperimentDetail(Experiment exp){
        this(exp.getID(), exp.getState(), exp.getLabel(), exp.getApplicationName());
    }

    /**
     * Creates the ExperimentDetail with explicit values for the necessary fields.
     *
     * @param id the id of the Experiment
     * @param state the state of the Experiment
     * @param label the Experiment label (name)
     * @param appName the name of the Application this Experiment belongs to
     */
    public ExperimentDetail(Experiment.ID id, Experiment.State state, Experiment.Label label, Application.Name appName){
        setId(id);
        setState(state);
        setLabel(label);
        setAppName(appName);
    }

    public Experiment.ID getId() {
        return id;
    }

    private void setId(Experiment.ID id) {
        if(id != null && !isEmpty(id.toString()))
            this.id = id;
        else
            throw new IllegalArgumentException("Can not create ExperimentDetail without an Experiment.ID");
    }

    public Experiment.State getState() {
        return state;
    }

    private void setState(Experiment.State state) {
        if(state != null)
            this.state = state;
        else throw new IllegalArgumentException("Experiment.State is not allowed to be null for ExperimentDetail");
    }

    public Experiment.Label getLabel() {
        return label;
    }

    private void setLabel(Experiment.Label label) {
        this.label = label;
    }

    public Application.Name getAppName() {
        return appName;
    }

    private void setAppName(Application.Name appName) {
        if(appName != null && !isEmpty(appName.toString()))
            this.appName = appName;
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
        if(totalNumberUsers > -1)
            this.totalNumberUsers = totalNumberUsers;
        else throw new IllegalArgumentException("Total number of users has to be equal or greater than zero");
    }

    /**
     * This method takes a list of buckets and transforms it to the {@link BucketDetail}s that are needed
     * for later extension.
     *
     * @param buckets a list of {@link Bucket}s
     */
    public void addBuckets(List<Bucket> buckets){

        List<BucketDetail> details = buckets.stream()
                .map(b -> new BucketDetail(b.getLabel(), b.isControl(), b.getAllocationPercent()))
                .collect(Collectors.toList());

        setBuckets(details);

    }


}
