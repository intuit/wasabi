package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.mapping.Result;
import com.datastax.driver.mapping.annotations.Accessor;
import com.datastax.driver.mapping.annotations.Query;
import com.intuit.wasabi.repository.cassandra.pojo.UserInfo;

@Accessor
public interface UserInfoAccessor {

    @Query("insert into user_info (user_id, user_email, firstname, lastname) values (?, ?, ?, ?)")
    ResultSet insertUserInfoBy(String userId, String userEmail, String firstName, String lastName);

    @Query("select * from user_info where user_id = ?")
    Result<UserInfo> getUserInfoBy(String userId);
}
