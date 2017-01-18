package com.intuit.wasabi.repository.cassandra.impl.authorization;

import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.accessor.AppRoleAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ApplicationListAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.UserInfoAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.UserRoleAccessor;
import com.intuit.wasabi.userdirectory.UserDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Guice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Guice(modules = CassandraRepositoryModule.class)
public class AuthorizationRepositorySetup {
    private final Logger logger = LoggerFactory.getLogger(AuthorizationRepositorySetup.class);
    @Inject MappingManager mappingManager;
    @Inject AppRoleAccessor appRoleAccessor;
    @Inject UserInfoAccessor userInfoAccessor;
    @Inject UserRoleAccessor userRoleAccessor;
    @Inject ApplicationListAccessor applicationListAccessor;
    @Inject UserDirectory userDirectory;

    protected final Set<String> apps = new HashSet<>();
    protected final Map<String, Set<String>> appUser = new HashMap<>();

    public void setupDb(String appName, String username, String role){
        UserInfo userInfo = userDirectory.lookupUser(UserInfo.Username.valueOf(username));
        logger.info(appName+" "+" "+username+" "+role + " "+userInfo);
        if(!"*".equals(appName)){
            apps.add(appName);
            Set<String> getOrNew = appUser.getOrDefault(appName, new HashSet<>());
            getOrNew.add(username);
            appUser.putIfAbsent(appName, getOrNew);
            applicationListAccessor.insert(appName);
        }
        logger.debug("inserted: "+appName);
        appRoleAccessor.insertAppRoleBy(appName, username, role);
        userInfoAccessor.insertUserInfoBy(username, userInfo.getEmail(), userInfo.getFirstName(), userInfo.getLastName());
        userRoleAccessor.insertUserRoleBy(username, appName, role);
    }

    @AfterTest
    public void cleanup(){
        logger.debug("cleaning up applicationlist table");
        mappingManager.getSession().execute("TRUNCATE TABLE applicationlist");
        logger.debug("cleaning up app_roles table");
        mappingManager.getSession().execute("TRUNCATE TABLE app_roles");
        logger.debug("cleaning up user_info table");
        mappingManager.getSession().execute("TRUNCATE TABLE user_info");
        logger.debug("cleaning up user_roles table");
        mappingManager.getSession().execute("TRUNCATE TABLE user_roles");
    }
}