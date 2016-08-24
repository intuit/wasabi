package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.index.PageExperimentByAppNamePage;

import java.util.UUID;

@Accessor
public interface ExperimentPageAccessor {
    @Query("insert into experiment_page(page, exp_id, assign) " +
            "values(?,?,?)")
    Statement insertBy(String page, UUID experimentId, boolean assign);

    @Query("delete from experiment_page where assign = ? and page = ? and exp_id = ?")
    Statement deleteBy(boolean assign, String page, UUID experimentId);

    @Query("delete from experiment_page where page = ? and exp_id = ?")
    Statement deleteBy(String page, UUID experimentId);

    @Query("select * from experiment_page where exp_id = ? and page = ?")
    Result<PageExperimentByAppNamePage> selectBy(UUID experimentId, String page);

    @Query("select * from experiment_page where exp_id = ?")
    Result<PageExperimentByAppNamePage> selectBy(UUID experimentId);

}
