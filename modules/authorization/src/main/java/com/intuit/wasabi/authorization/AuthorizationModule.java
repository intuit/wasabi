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
package com.intuit.wasabi.authorization;

import com.google.inject.AbstractModule;
import com.intuit.wasabi.authentication.AuthenticationModule;
import com.intuit.wasabi.eventlog.EventLogModule;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import org.slf4j.Logger;

import java.util.Properties;

import static com.google.inject.Scopes.SINGLETON;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static java.lang.Class.forName;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Module which binds the authZ interface to the authZ implementation
 */
public class AuthorizationModule extends AbstractModule {

    private static final String PROPERTY_NAME = "/authorization.properties";
    private static final Logger LOGGER = getLogger(AuthorizationModule.class);
    
    /*
     * @see com.google.inject.AbstractModule#configure()
     */

    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", AuthorizationModule.class.getSimpleName());

        install(new AuthenticationModule());
        install(new EventLogModule());
        install(new CassandraRepositoryModule());

        Properties properties = create(PROPERTY_NAME, AuthorizationModule.class);
        String authorizationClassName = getProperty("authorization.class.name", properties,
                "com.intuit.wasabi.authorization.impl.DefaultAuthorization");

        try {
            @SuppressWarnings("unchecked")
            Class<Authorization> authorizationClass = (Class<Authorization>) forName(authorizationClassName);

            bind(Authorization.class).to(authorizationClass).in(SINGLETON);
        } catch (ClassNotFoundException e) {
            throw new AuthenticationException("unable to find authorization class: " + authorizationClassName, e);
        }

        LOGGER.debug("installed module: {}", AuthorizationModule.class.getSimpleName());
    }
}
