package com.intuit.wasabi.auditlog;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link AuditLogModule}.
 */
public class AuditLogModuleTest {

    @Before
    @After
    public void setSysProps() {
        System.getProperties().setProperty("auditlog.listener.class.name", "com.intuit.wasabi.auditlog.impl.AuditLogListenerImpl");
        System.getProperties().setProperty("auditlog.implementation.class.name", "com.intuit.wasabi.auditlog.impl.AuditLogImpl");
    }

    @Test
    public void testConfigureFailListener() throws Exception {
        System.getProperties().setProperty("auditlog.listener.class.name", "nonExistentListener");
        try {
            Injector injector = Guice.createInjector(new AuditLogModule());
            injector.getInstance(AuditLogListener.class);
            Assert.fail();
        } catch (CreationException ignored) {
        }

    }

    @Test
    public void testConfigureFailImpl() throws Exception {
        System.getProperties().setProperty("auditlog.implementation.class.name", "nonExistentImplementation");
        try {
            Injector injector = Guice.createInjector(new AuditLogModule());
            injector.getInstance(AuditLog.class);
            Assert.fail();
        } catch (CreationException ignored) {
        }
    }

    @Test
    public void testConfigure() throws Exception {
        try {
            Guice.createInjector(new AuditLogModule());
        } catch (CreationException ignored) {
        }
    }
}
