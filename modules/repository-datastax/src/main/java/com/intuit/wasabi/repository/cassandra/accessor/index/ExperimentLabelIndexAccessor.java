package com.intuit.wasabi.repository.cassandra.accessor.index;

import com.datastax.driver.mapping.annotations.Accessor;

@Accessor
public interface ExperimentLabelIndexAccessor {

//    @Query("select * from experiment_label_index where appName = ? and label = ?")
//    Experiment getExperimentBy(String appName, String Label);
}