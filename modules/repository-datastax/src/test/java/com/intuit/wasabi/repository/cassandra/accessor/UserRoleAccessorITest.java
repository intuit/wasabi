/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.Result;
import com.intuit.wasabi.repository.cassandra.IntegrationTestBase;
import com.intuit.wasabi.repository.cassandra.pojo.UserRole;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserRoleAccessorITest extends IntegrationTestBase {
    private static final String SUPERADMIN = "superadmin";
    private static final String APP_NAME_ALL = "*";
    static UserRoleAccessor accessor;
    static Mapper<UserRole> mapper;
    private static String userId = "userId1" + System.currentTimeMillis();

    @BeforeClass
    public static void setup() {
        IntegrationTestBase.setup();
        if (accessor != null) return;
        accessor = injector.getInstance(UserRoleAccessor.class);

        session.execute("delete from wasabi_experiments.user_role where user_id = '" + userId + "'");
    }

    @Test
    public void testCreateAndGetAndDeleteSuperAdmin() {
        Result<UserRole> user = accessor.getUserRolesByUserId(userId);
        assertEquals("Size should be zero", 0, user.all().size());
        Result<UserRole> userRoles = accessor.getAllUserRoles();
        int countBefore = userRoles.all().size();
        accessor.insertUserRoleBy(userId, APP_NAME_ALL, SUPERADMIN);
        user = accessor.getUserRolesByUserId(userId);
        assertEquals("Size should be zero", 1, user.all().size());
        int countAfter = accessor.getAllUserRoles().all().size();
        assertEquals("count should be increased", countAfter, countBefore + 1);

        UserRole role = user.one();
        assertEquals(userId, role.getUserId());
        assertEquals(APP_NAME_ALL, role.getAppName());
        assertEquals(SUPERADMIN, role.getRole());

        accessor.deleteUserRoleBy(userId, APP_NAME_ALL);
        user = accessor.getUserRolesByUserId(userId);
        assertEquals("Size should be zero", 0, user.all().size());
    }

}