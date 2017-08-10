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
package com.intuit.wasabi.authentication;

import com.google.inject.AbstractModule;
import com.intuit.wasabi.authentication.impl.NoOpAuthenticateByHttpRequestImpl;
import com.intuit.wasabi.exceptions.AuthenticationException;
import org.slf4j.Logger;

import java.util.Properties;

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.name.Names.named;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static java.lang.Class.forName;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Guice module for authentication module
 */
public class AuthenticationModule extends AbstractModule {

    public static final String PROPERTY_NAME = "/authentication.properties";
    private static final Logger LOGGER = getLogger(AuthenticationModule.class);

    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", AuthenticationModule.class.getSimpleName());

        Properties properties = create(PROPERTY_NAME, AuthenticationModule.class);

        bind(String.class).annotatedWith(named("authentication.scheme"))
                .toInstance(getProperty("authentication.scheme", properties));
        bind(String.class).annotatedWith(named("authentication.token.scheme"))
                .toInstance(getProperty("authentication.token.scheme", properties));
        bind(String.class).annotatedWith(named("authentication.token.salt"))
                .toInstance(getProperty("authentication.token.salt", properties));
        bind(String.class).annotatedWith(named("authentication.token.version"))
                .toInstance(getProperty("authentication.token.version", properties));
        bind(String.class).annotatedWith(named("authentication.http.proxy.host"))
                .toInstance(getProperty("http.proxy.host", properties));
        bind(Integer.class).annotatedWith(named("authentication.http.proxy.port"))
                .toInstance(Integer.parseInt(getProperty("http.proxy.port", properties, "80")));

        String authenticationClassName = getProperty("authentication.class.name", properties,
                "com.intuit.wasabi.authentication.impl.DefaultAuthentication");

        try {
            Class<Authentication> authImplClass = (Class<Authentication>) forName(authenticationClassName);

            bind(Authentication.class).to(authImplClass).in(SINGLETON);
            bind(AuthenticateByHttpRequest.class).to(NoOpAuthenticateByHttpRequestImpl.class).asEagerSingleton();
        } catch (ClassNotFoundException e) {
            LOGGER.error("unable to find class: {}", authenticationClassName, e);

            throw new AuthenticationException("unable to find class: " + authenticationClassName, e);
        }

        LOGGER.debug("installed module: {}", AuthenticationModule.class.getSimpleName());
    }
}
