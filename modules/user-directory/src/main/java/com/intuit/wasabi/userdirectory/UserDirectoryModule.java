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
package com.intuit.wasabi.userdirectory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.exceptions.UserToolsException;
import org.slf4j.Logger;

import java.util.List;
import java.util.Properties;

import static com.google.inject.Scopes.SINGLETON;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static com.intuit.wasabi.userdirectory.UserDirectoryAnnotations.USER_DIRECTORY_PATH;
import static java.lang.Class.forName;
import static org.slf4j.LoggerFactory.getLogger;

public class UserDirectoryModule extends AbstractModule {

    public static final String PROPERTY_NAME = "/userDirectory.properties";
    private static final Logger LOGGER = getLogger(UserDirectoryModule.class);

    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", UserDirectoryModule.class.getSimpleName());

        Properties properties = create(PROPERTY_NAME, UserDirectoryModule.class);
        String userToolsClassName = getProperty("user.lookup.class.name", properties,
                "com.intuit.wasabi.userdirectory.impl.DefaultUserDirectory");

        bind(String.class).annotatedWith(Names.named(USER_DIRECTORY_PATH))
                .toInstance(PROPERTY_NAME);
        bind(new TypeLiteral<List<UserInfo>>() {
        }).annotatedWith(Names.named("authentication.users"))
                .toProvider(UserInfoListProvider.class).in(SINGLETON);
        try {
            @SuppressWarnings("unchecked")
            Class<UserDirectory> userToolsClass = (Class<UserDirectory>) forName(userToolsClassName);

            bind(UserDirectory.class).to(userToolsClass).in(SINGLETON);
        } catch (ClassNotFoundException e) {
            LOGGER.error("unable to find class: {}", userToolsClassName, e);

            throw new UserToolsException("unable to find class: " + userToolsClassName, e);
        }

        LOGGER.debug("installed module: {}", UserDirectoryModule.class.getSimpleName());
    }

//    @Provides
//    public @Named("authentication.users") List<UserInfo> provideUsers() {
//        Properties properties = create(PROPERTY_NAME, UserDirectoryModule.class);
//        String userIds = getProperty("user.ids", properties);
//        List<UserInfo> users = new ArrayList<UserInfo>();
//
//        for (String userId : userIds.split(":")) {
//            userId = trimToNull(userId);
//
//            // format userId:username:password:email:firstname:lastname
//            if (userId != null) {
//                String userCredentials = getProperty("user." + userId, properties);
//
//                userCredentials = trimToNull(userCredentials);
//
//                if (userCredentials != null) {
//                    final String[] userCredential = userCredentials.split(":", -1);
//
//                    users.add(new UserInfo.Builder(UserInfo.Username.valueOf(userCredential[0]))
//                            .withUserId(userId)
//                            .withPassword(userCredential[1])
//                            .withEmail(userCredential[2])
//                            .withFirstName(userCredential[3])
//                            .withLastName(userCredential[4])
//                            .build());
//                }
//            }
//        }
//
//        return users;
//    }
}
