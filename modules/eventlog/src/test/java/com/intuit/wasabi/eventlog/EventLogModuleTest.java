/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.eventlog;

import com.google.inject.CreationException;
import org.junit.Assert;
import org.junit.Test;

import static com.google.inject.Guice.createInjector;
import static java.lang.System.setProperty;

/**
 * Tests for {@link EventLogModule}.
 */

public class EventLogModuleTest {

    @Test
    public void testConfigure() throws Exception {
        setProperty("eventlog.class.name", "com.intuit.wasabi.eventlog.impl.EventLogImpl");
        try {
            createInjector(new EventLogModule()).getInstance(EventLogModule.class);
        } catch (CreationException exception) {
            Assert.fail("Should have found an instance!");
        }
    }

    @Test(expected = CreationException.class)
    public void testConfigureFail() throws Exception {
        setProperty("eventlog.class.name", "nonexistentclass");

        createInjector(new EventLogModule()).getInstance(EventLogModule.class);

        setProperty("eventlog.class.name", "com.intuit.wasabi.eventlog.impl.EventLogImpl");
    }
}
