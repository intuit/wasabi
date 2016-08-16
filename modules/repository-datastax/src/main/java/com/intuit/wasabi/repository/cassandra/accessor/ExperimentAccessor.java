package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.Experiment;

import java.util.Date;
import java.util.UUID;

@Accessor
public interface ExperimentAccessor {
    //supported by the default query since it is primary key selection
//    @Query("select * from experiment where id = ?")
//    Experiment getExperimentBy(UUID experimentID);

    @Query("update experiment set state = ?, modified = ? where id = ?")
    ResultSet updateExperiment(String state, Date modifiedOn, UUID uuid);

    @Query("select * from experiment where appName = ?")
    Result<Experiment> getExperimentBy(String appName);


}
