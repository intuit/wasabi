/*******************************************************************************
 * Copyright 2016 Intuit
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.cassandra.data;

import com.intuit.wasabi.authorizationobjects.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class AuthorizationRepositoryDataProvider {
    private final Logger logger = LoggerFactory.getLogger(AuthorizationRepositoryDataProvider.class);
    /*
      app_name text,
      user_id text,
      role text,
     */

    @DataProvider(name = "AuthorizationDataProvider")
    public static Object[][] getExperimentTimes() {
        return new Object[][]{
                new Object[]{
                        "App1",
                        "wasabi_writer",
                        Role.READWRITE.name()
                },
                new Object[]{
                        "App2",
                        "wasabi_writer",
                        Role.READWRITE.name()
                },
                new Object[]{
                        "App1",
                        "wasabi_reader",
                        Role.READONLY.name()
                },
                new Object[]{
                        "*",
                        "admin",
                        Role.SUPERADMIN.name()
                }
        };
    }

}