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
package com.intuit.wasabi.auditlog;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.intuit.wasabi.eventlog.EventLogModule;
import com.intuit.wasabi.exceptions.AuditLogException;
import org.slf4j.Logger;

import java.util.Properties;

import static com.google.inject.name.Names.named;
import static com.intuit.autumn.utils.PropertyFactory.create;
import static com.intuit.autumn.utils.PropertyFactory.getProperty;
import static com.intuit.wasabi.auditlog.AuditLogAnnotations.AUDITLOG_FETCHLIMIT;
import static com.intuit.wasabi.auditlog.AuditLogAnnotations.AUDITLOG_THREADPOOLSIZE_CORE;
import static com.intuit.wasabi.auditlog.AuditLogAnnotations.AUDITLOG_THREADPOOLSIZE_MAX;
import static java.lang.Class.forName;
import static java.lang.Integer.parseInt;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Module for providing an AuditLogListener.
 */
public class AuditLogModule extends AbstractModule {

    public static final String PROPERTY_NAME = "/auditlog.properties";
    private static final Logger LOGGER = getLogger(AuditLogModule.class);

    /**
     * Configures a {@link Binder} via the exposed methods.
     */
    @Override
    protected void configure() {
        LOGGER.debug("installing module: {}", AuditLogModule.class.getSimpleName());

        install(new EventLogModule());

        Properties properties = create(PROPERTY_NAME, AuditLogModule.class);

        bind(Integer.class).annotatedWith(named(AUDITLOG_THREADPOOLSIZE_CORE)).toInstance(
                parseInt(getProperty("auditlog.threadpoolsize.core", properties, "2")));
        bind(Integer.class).annotatedWith(named(AUDITLOG_THREADPOOLSIZE_MAX)).toInstance(
                parseInt(getProperty("auditlog.threadpoolsize.max", properties, "4")));

        String auditLogListenerClass = getProperty("auditlog.listener.class.name", properties,
                "com.intuit.wasabi.auditlog.impl.NoopAuditLogListenerImpl");

        try {
            @SuppressWarnings("unchecked")
            Class<AuditLogListener> auditLogListenerImplClass = (Class<AuditLogListener>) forName(auditLogListenerClass);

            bind(AuditLogListener.class).to(auditLogListenerImplClass).asEagerSingleton();
        } catch (ClassNotFoundException e) {
            throw new AuditLogException("unable to find class: " + auditLogListenerClass, e);
        }

        bind(Integer.class).annotatedWith(named(AUDITLOG_FETCHLIMIT)).toInstance(
                parseInt(getProperty("auditlog.fetchlimit", properties, "10000")));

        String auditLogClass = getProperty("auditlog.implementation.class.name", properties,
                "com.intuit.wasabi.auditlog.impl.NoopAuditLogImpl");

        try {
            @SuppressWarnings("unchecked")
            Class<AuditLog> auditLogImplClass = (Class<AuditLog>) forName(auditLogClass);

            bind(AuditLog.class).to(auditLogImplClass).asEagerSingleton();
        } catch (ClassNotFoundException e) {
            throw new AuditLogException("unable to find class: " + auditLogClass, e);
        }

        LOGGER.debug("installed module: {}", AuditLogModule.class.getSimpleName());
    }
}
