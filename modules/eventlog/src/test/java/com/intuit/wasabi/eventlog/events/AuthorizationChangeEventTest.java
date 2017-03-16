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
package com.intuit.wasabi.eventlog.events;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.experimentobjects.Application;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class AuthorizationChangeEventTest {

    @Test
    public void testGetDefaultDescription() throws Exception {
        UserInfo affectedUser = UserInfo.from(UserInfo.Username.valueOf("AffectedUser")).withUserId("AffectedUser").withFirstName("Aff").withLastName("Ected").build();
        UserInfo invokingUser = UserInfo.from(UserInfo.Username.valueOf("InvokingUser")).withUserId("InvokingUser").withFirstName("Inv").withLastName("Oking").build();

        AuthorizationChangeEvent event1 = new AuthorizationChangeEvent(invokingUser, Application.Name.valueOf("TestApp"), affectedUser, "", "ADMIN");
        Assert.assertTrue("Description does not contain the application name, was:\n" + event1.getDefaultDescription(),
                event1.getDefaultDescription().contains("TestApp"));
        Assert.assertTrue("Description does not contain the username (affected user), was:\n" + event1.getDefaultDescription(),
                event1.getDefaultDescription().contains(affectedUser.getUsername().toString()));
        Assert.assertTrue("Description does not contain the username (invoking user), was:\n" + event1.getDefaultDescription(),
                event1.getDefaultDescription().contains(invokingUser.getUsername().toString()));
        Assert.assertTrue("Description does not contain the role change, was:\n" + event1.getDefaultDescription(),
                event1.getDefaultDescription().contains("ADMIN"));

        AuthorizationChangeEvent event2 = new AuthorizationChangeEvent(invokingUser, Application.Name.valueOf("TestApp"), affectedUser, "", "");
        Assert.assertTrue("Description does not contain the application name, was:\n" + event1.getDefaultDescription(),
                event2.getDefaultDescription().contains("TestApp"));
        Assert.assertTrue("Description does not contain the username (affected user), was:\n" + event1.getDefaultDescription(),
                event2.getDefaultDescription().contains(affectedUser.getUsername().toString()));
        Assert.assertTrue("Description does not contain the username (invoking user), was:\n" + event1.getDefaultDescription(),
                event2.getDefaultDescription().contains(invokingUser.getUsername().toString()));
        Assert.assertTrue("Description does not contain the role deletion (\"removed\"), was:\n" + event1.getDefaultDescription(),
                event2.getDefaultDescription().contains("removed"));
    }

    @Test
    public void testGetApplicationName() throws Exception {
        AuthorizationChangeEvent event = new AuthorizationChangeEvent(Application.Name.valueOf("TestApp"), EventLog.SYSTEM_USER, "", "ADMIN");
        Assert.assertEquals(Application.Name.valueOf("TestApp"), event.getApplicationName());

    }

    @Test
    public void testChangeProperties() throws Exception {
        AuthorizationChangeEvent event = new AuthorizationChangeEvent(Application.Name.valueOf("TestApp"), EventLog.SYSTEM_USER, "", "ADMIN");
        AbstractChangeEventTest.testValidSystemEvent(event, "System User (system_user)", "", "ADMIN");
    }
}
