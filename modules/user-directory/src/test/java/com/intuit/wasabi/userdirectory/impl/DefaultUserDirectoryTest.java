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
package com.intuit.wasabi.userdirectory.impl;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.exceptions.AuthenticationException;
import com.intuit.wasabi.userdirectory.UserDirectory;
import com.intuit.wasabi.userdirectory.UserDirectoryModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class DefaultUserDirectoryTest {

    private UserDirectory userDirectory;

    @Before
    public void setUp() throws Exception {
        System.getProperties().put("user.lookup.class.name",
                "com.intuit.wasabi.userdirectory.impl.DefaultUserDirectory");
        Injector injector = Guice.createInjector(new UserDirectoryModule());
        userDirectory = injector.getInstance(UserDirectory.class);
    }

    @Test(expected = AuthenticationException.class)
    public void testLookupByEmailDummy() {
        assertNull(userDirectory.lookupUserByEmail("dummy@dummy.com"));
    }

    @Test(expected = AuthenticationException.class)
    public void testLookUserDummy() {
        assertNull(userDirectory.lookupUser(UserInfo.Username.valueOf("dummy")));
    }

    @Test
    public void testLookupByEmailSuccess() {
        assertNotNull(userDirectory.lookupUserByEmail("admin@example.com"));
        assertNotNull(userDirectory.lookupUserByEmail("wasabi_writer@example.com"));
        assertNotNull(userDirectory.lookupUserByEmail("wasabi_admin@example.com"));
        assertNotNull(userDirectory.lookupUserByEmail("wasabi_reader@example.com"));
    }

    @Test
    public void testExistingUser() {
        UserInfo user = userDirectory.lookupUserByEmail("admin@example.com");
        assertNotNull(user);
        assertThat(user.getEmail(), is("admin@example.com"));
        assertThat(user.getUsername().getUsername(), is("admin"));
        assertThat(user.getFirstName(), is("Wasabi"));
        assertThat(user.getLastName(), is("Admin"));

        user = userDirectory.lookupUserByEmail("wasabi_admin@example.com");
        assertThat(user.getEmail(), is("wasabi_admin@example.com"));
        assertThat(user.getUsername().getUsername(), is("wasabi_admin"));
        assertThat(user.getFirstName(), is("Wasabi"));
        assertThat(user.getLastName(), is("Admin"));

        user = userDirectory.lookupUserByEmail("wasabi_reader@example.com");
        assertThat(user.getEmail(), is("wasabi_reader@example.com"));
        assertThat(user.getUsername().getUsername(), is("wasabi_reader"));
        assertThat(user.getFirstName(), is("Wasabi"));
        assertThat(user.getLastName(), is("Reader"));

        user = userDirectory.lookupUserByEmail("wasabi_writer@example.com");
        assertThat(user.getEmail(), is("wasabi_writer@example.com"));
        assertThat(user.getUsername().getUsername(), is("wasabi_writer"));
        assertThat(user.getFirstName(), is("Wasabi"));
        assertThat(user.getLastName(), is("Writer"));
    }

    @Test
    public void testLookupUserSuccess() {
        UserInfo user = userDirectory.lookupUser(UserInfo.Username.valueOf("admin"));
        assertNotNull(user);
        assertThat(user.getEmail(), is("admin@example.com"));
        assertThat(user.getUsername().getUsername(), is("admin"));
        assertThat(user.getFirstName(), is("Wasabi"));
        assertThat(user.getLastName(), is("Admin"));

        user = userDirectory.lookupUser(UserInfo.Username.valueOf("wasabi_admin"));
        assertThat(user.getEmail(), is("wasabi_admin@example.com"));
        assertThat(user.getUsername().getUsername(), is("wasabi_admin"));
        assertThat(user.getFirstName(), is("Wasabi"));
        assertThat(user.getLastName(), is("Admin"));

        user = userDirectory.lookupUser(UserInfo.Username.valueOf("wasabi_reader"));
        assertThat(user.getEmail(), is("wasabi_reader@example.com"));
        assertThat(user.getUsername().getUsername(), is("wasabi_reader"));
        assertThat(user.getFirstName(), is("Wasabi"));
        assertThat(user.getLastName(), is("Reader"));

        user = userDirectory.lookupUser(UserInfo.Username.valueOf("wasabi_writer"));
        assertThat(user.getEmail(), is("wasabi_writer@example.com"));
        assertThat(user.getUsername().getUsername(), is("wasabi_writer"));
        assertThat(user.getFirstName(), is("Wasabi"));
        assertThat(user.getLastName(), is("Writer"));
    }
}
