package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.AppRole;

@Accessor
public interface AppRoleAccessor {

    @Query("select * from app_roles where app_name = ?")
    Result<AppRole> getAppRoleByAppName(String appName);

    @Query("delete from app_roles where app_name = ? and user_id = ?")
    ResultSet deleteAppRoleBy(String appName, String userId);

    @Query("insert into app_roles (app_name, user_id, role) values (?, ?, ?)")
    ResultSet insertAppRoleBy(String appName, String userId, String role);

    //TODO: this is a hack for multitable batch statement
    @Query("insert into app_roles (app_name, user_id, role) values (?, ?, ?)")
    Statement insertAppRoleStatement(String appName, String userId, String role);

    @Query("delete from app_roles where app_name = ? and user_id = ?")
    Statement deleteAppRoleStatement(String appName, String userId);
}