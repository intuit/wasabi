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
package com.intuit.wasabi.authenticationobjects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class LoginCredentialsTest {

    private UserInfo.Username username = UserInfo.Username.valueOf("testUser");
    private String password = "testPasswd";
    private String namespaceId = "testNameSpace";

    private LoginCredentials creds;

    @Before
    public void setUp() throws Exception {
        creds = getLoginCredentials();
    }

    /**
     * @return
     */
    private LoginCredentials getLoginCredentials() {
        return LoginCredentials.withUsername(username)
                                .withNamespaceId(namespaceId)
                                .withPassword(password)
                                .build();
    }

    @Test
    public void testCredentialsSet() {
        creds.setNamespaceId(namespaceId);
        creds.setPassword(password);
        creds.setUsername(username);
        assertEquals(namespaceId, creds.getNamespaceId());
        assertEquals(password, creds.getPassword());
        assertEquals(username, creds.getUsername());
    }

    @Test
    public void testHashCodeAndEquals(){
        LoginCredentials cred1 = getLoginCredentials();
        LoginCredentials cred2 = getLoginCredentials();
        assertThat(cred1.equals(cred2), is(true));
        assertThat(cred1.hashCode(), is(cred2.hashCode()));

    }

}
