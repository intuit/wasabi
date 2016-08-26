package com.intuit.wasabi.repository.cassandra.accessor;

import java.util.List;
import java.util.UUID;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.Bucket;

@Accessor
public interface BucketAccessor {

    @Query("select * from bucket where experiment_id IN ?")
    Result<Bucket> getBucketByExperimentIds(List<UUID> experimentIds);

    @Query("select * from bucket where experiment_id = ?")
    Result<Bucket> getBucketByExperimentId(UUID experimentId);

    @Query("insert into bucket (experiment_id, label, description, "
    		+ "allocation, is_control, payload, state) " +
                "values (?, ?, ?, ?, ?, ?, ?)")
    void insert(UUID experimentId, String label, String description, 
    		double allocation, boolean isControl, String payload, String state);
    
    @Query("delete from bucket where experiment_id = ?")
    void deleteByExperimentId(UUID experimentId);
    
    @Query("delete from bucket where experiment_id = ? and label = ?")
    void deleteByExperimentIdAndLabel(UUID experimentId, String label);

    @Query("update bucket set state = ? where experiment_id = ? and label = ?")
	void updateState(String name, UUID rawID, String string);

    @Query("select * from bucket where experiment_id = ? and label = ?")
	Result<Bucket> getBucketByExperimentIdAndBucket(UUID rawID, String string);
    
}
