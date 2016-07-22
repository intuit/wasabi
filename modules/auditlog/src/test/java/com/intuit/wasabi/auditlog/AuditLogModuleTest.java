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
