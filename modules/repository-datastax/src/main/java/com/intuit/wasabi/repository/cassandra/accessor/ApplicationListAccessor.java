package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.ApplicationList;

@Accessor
public interface ApplicationListAccessor {

    @Query("select distinct app_name from applicationList")
    Result<ApplicationList> getUniqueAppName();
}
