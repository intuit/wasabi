package com.intuit.wasabi.repository.cassandra.accessor.index;

import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.AppPage;

@Accessor
public interface AppPageIndexAccessor {
    @Query("insert into app_page_index(app_name, page) " +
            "values(?,?)")
    Statement insertBy(String appName, String page);

    @Query("delete from app_page_index where page = ? and app_name = ?")
    Statement deleteBy(String page, String appName);

    @Query("select * from app_page_index where page = ? and app_name = ?")
    Result<AppPage> selectBy(String page, String appName);

    @Query("select * from app_page_index where app_name = ?")
    Result<AppPage> selectBy(String appName);

    @Query("update app_page_index set page = ? where app_name = ?")
    Statement updatePageBy(String page, String appName);
}
