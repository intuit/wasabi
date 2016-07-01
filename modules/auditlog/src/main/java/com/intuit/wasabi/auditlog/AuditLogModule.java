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

        bind(Integer.class).annotatedWith(named("auditlog.threadpoolsize.core")).toInstance(
                parseInt(getProperty("auditlog.threadpoolsize.core", properties, "2")));
        bind(Integer.class).annotatedWith(named("auditlog.threadpoolsize.max")).toInstance(
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

        bind(Integer.class).annotatedWith(named("auditlog.fetchlimit")).toInstance(
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
