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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.exceptions.UserToolsException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 *
 */
public class UserDirectoryModuleTest {

    @Test
    public void testConfigureDefaultClassFound() throws Exception {
        System.getProperties().put("user.lookup.class.name",
                "com.intuit.wasabi.userdirectory.impl.DefaultUserDirectory");
        Injector injector = Guice.createInjector(new UserDirectoryModule());
        UserDirectory userDirectory = injector.getInstance(UserDirectory.class);
        assertNotNull(userDirectory);
    }

    @Test
    public void testProvidesUsers() throws Exception {
        System.getProperties().put("user.lookup.class.name",
                "com.intuit.wasabi.userdirectory.impl.DefaultUserDirectory");
        Injector injector = Guice.createInjector(new UserDirectoryModule());
        List<UserInfo> users = injector.getInstance(Key.get(new TypeLiteral<List<UserInfo>>() {
                                                            },
                Names.named("authentication.users")));
        assertEquals(4, users.size());
    }

    @Test
    public void testConfigureDummyClassNotFound() throws Exception {
        System.getProperties().put("user.lookup.class.name",
                "dummy.class.name");
        try {
            Injector injector = Guice.createInjector(new UserDirectoryModule());
            // should not happen
            fail();
            assertNull(injector);
        } catch (Exception e) {
            assertEquals(e.getCause().getClass(), UserToolsException.class);
        }
    }
}
