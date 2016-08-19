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