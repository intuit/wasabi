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
import com.intuit.wasabi.eventlog.EventLogEventType;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Calendar;

/**
 * Tests for {@link AbstractEvent} and a test method for subclasses.
 */
public class AbstractEventTest {

    @Test
    public void testAbstractEvent() throws Exception {
        UserInfo user = Mockito.mock(UserInfo.class);
        AbstractEvent systemEvent = new AbstractEvent(null) {
            @Override
            public String getDefaultDescription() {
                return "Desc";
            }
        };
        testValidSystemEvent(systemEvent);

        systemEvent = new AbstractEvent(user) {
            @Override
            public String getDefaultDescription() {
                return "Desc";
            }
        };
        Assert.assertEquals(user, systemEvent.getUser());
        Assert.assertEquals(EventLogEventType.UNKNOWN, systemEvent.getType());
    }

    /**
     * Tests an event for its for its inherited features (that means if the properties are passed on correctly).
     * Assumes creationTime to be in [now-3s, now+3s], and that the event is caused by the {@link EventLog#SYSTEM_USER}.
     * The description must not be {@code null}.
     *
     * @param event The event to check.
     */
    public static void testValidSystemEvent(AbstractEvent event) {
        Assert.assertEquals("not the system user", EventLog.SYSTEM_USER, event.getUser());

        Calendar soon = Calendar.getInstance();
        soon.add(Calendar.SECOND, 3);
        Assert.assertTrue("Event time off (too late)", event.getTime().before(soon));

        Calendar past = Calendar.getInstance();
        past.add(Calendar.SECOND, -3);
        Assert.assertTrue("Event time off (too early)", event.getTime().after(past));

        Assert.assertNotNull("Event description is null!", event.getDefaultDescription());
    }

}
