package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.UserRole;


@Accessor
public interface UserRoleAccessor {

    @Query("select * from user_roles where user_id = ? and app_name = ?")
    Result<UserRole> getUserRolesBy(String userId, String appName);

    @Query("select * from user_roles where user_id = ? and app_name = '*'")
    Result<UserRole> getUserRolesByUserIdWithWildcardAppName(String userId);

    @Query("select * from user_roles where user_id = ?")
    Result<UserRole> getUserRolesByUserId(String userId);

    @Query("delete from user_roles where user_id = ? and app_name = ?")
    ResultSet deleteUserRoleBy(String userId, String appName);

    @Query("insert into user_roles (user_id, app_name, role) values (?, ?, ?)")
    ResultSet insertUserRoleBy(String userId, String appName, String role);

    //TODO: this is a hack for multitable batch statement
    @Query("insert into user_roles (user_id, app_name, role) values (?, ?, ?)")
    Statement insertUserRoleStatement(String userId, String appName, String role);

    @Query("delete from user_roles where user_id = ? and app_name = ?")
    Statement deleteUserRoleStatement(String userId, String appName);

}