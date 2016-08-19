package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;

@Accessor
public interface StagingAccessor {
    @Query("insert into staging(time, type, exep , msg) values(now(), 'ASSIGNMENT', ? , ?)")
    ResultSet insertBy(String exception, String message);
}