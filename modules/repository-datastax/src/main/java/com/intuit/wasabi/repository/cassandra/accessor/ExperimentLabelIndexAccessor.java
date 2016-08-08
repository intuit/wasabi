package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.mapping.annotations.Accessor;

@Accessor
public interface ExperimentLabelIndexAccessor {

//    @Query("select * from experiment_label_index where app_name = ? and label = ?")
//    Experiment getExperimentBy(String appName, String Label);
}