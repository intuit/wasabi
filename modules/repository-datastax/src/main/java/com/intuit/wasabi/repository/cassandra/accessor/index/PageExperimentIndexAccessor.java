package com.intuit.wasabi.repository.cassandra.accessor.index;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.index.PageExperimentByAppNamePage;

import java.util.UUID;

@Accessor
public interface PageExperimentIndexAccessor {
    @Query("insert into page_experiment_index(app_name, page, exp_id, assign) " +
            "values(?,?,?,?);")
    Statement insertBy(String appName, String page, UUID experimentId, boolean assign);

    @Query("delete from page_experiment_index where app_name = ? and page = ? and exp_id = ?")
    ResultSet deleteBy(String appName, String page, UUID uuid);

    @Query("select * from page_experiment_index where app_name = ? and page = ?")
    Result<PageExperimentByAppNamePage> selectBy(String appName, String page);

}
