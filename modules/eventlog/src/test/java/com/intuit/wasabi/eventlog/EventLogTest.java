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
package com.intuit.wasabi.eventlog;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link EventLog}.
 */
public class EventLogTest {

    @Test
    public void testSystemUser() {
        assertEquals(UserInfo.Username.valueOf("SYSTEM_USER"), EventLog.SYSTEM_USER.getUsername());
        assertEquals("System", EventLog.SYSTEM_USER.getFirstName());
        assertEquals("User", EventLog.SYSTEM_USER.getLastName());
        assertEquals("SystemUser", EventLog.SYSTEM_USER.getUserId());
        assertEquals("admin@example.com", EventLog.SYSTEM_USER.getEmail());
    }
}
