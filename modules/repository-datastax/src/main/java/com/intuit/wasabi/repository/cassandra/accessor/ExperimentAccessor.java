package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.Experiment;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Accessor
public interface ExperimentAccessor {

	@Query("select * from experiment where id = ?")
    Result<Experiment> getExperimentById(UUID experimentID);

	@Query("delete from experiment where id = ?")
	void deleteExperiment(UUID id);
	
    @Query("update experiment set state = ?, modified = ? where id = ?")
    ResultSet updateExperiment(String state, Date modifiedOn, UUID experimentId);

    @Query("select * from experiment where app_name = ?")
    Result<Experiment> getExperimentByAppName(String appName);

    @Query("select * from experiment where id = ?")
    Result<Experiment> selectBy(UUID experimentId);

    @Query("insert into experiment " +
                "(id, description, rule, sample_percent, start_time, end_time, " +
                "   state, label, app_name, created, modified, is_personalized, model_name, model_version," +
                " is_rapid_experiment, user_cap, creatorid) " +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
    void insertExperiment(UUID id, String description, String rule, double sample_percent, 
    		Date start_time, Date end_time, String state, String label, String app_name, 
    		Date created, Date modified, boolean is_personalized, String model_name, 
    		String model_version, boolean is_rapid_experiment, int user_cap, String creatorid);
}
