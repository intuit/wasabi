package com.intuit.wasabi.repository.cassandra.accessor.index;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.index.ExperimentByAppNameLabel;

import java.util.Date;
import java.util.UUID;

@Accessor
public interface ExperimentLabelIndexAccessor {

    @Query("select * from experiment_label_index where appName = ? and label = ?")
    Result<ExperimentByAppNameLabel> getExperimentBy(String appName, String Label);

    @Query("update experiment_label_index set id = ?, modified = ?, start_time = ?, end_time = ?, state = ? " +
            "where app_name = ? and label = ?")
    ResultSet updateBy(UUID uuid, Date modified, Date startTime, Date endTime, String state, String appName, String label);

    @Query("delete from experiment_label_index where app_name = ? and label = ?")
    ResultSet deleteBy(String appName, String label);
}