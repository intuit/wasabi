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
package com.intuit.wasabi.email;

import com.intuit.wasabi.email.impl.EmailEventLogListener;
import com.intuit.wasabi.email.impl.EmailTextProcessorImpl;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of the {@link com.intuit.wasabi.email.impl.EmailEventLogListener}
 *
 */
public class EmailEventLogListenerTest {

    @Test
    public void testSendMessage() {
        // mocking and getting all the dependencies
        EventLog eventLog = mock(EventLog.class);
        EmailService emailService = mock(EmailService.class);
        EmailTextProcessor emailTextProcessor = mock(EmailTextProcessorImpl.class);

        EmailEventLogListener emailListener = new EmailEventLogListener(eventLog, emailService, emailTextProcessor);

        EventLogEvent mockedEvent = mock(EventLogEvent.class);

        emailListener.postEvent(mockedEvent);

        verify(emailTextProcessor).getSubject(mockedEvent);
        verify(emailTextProcessor).getMessage(mockedEvent);
        verify(emailTextProcessor).getAddressees(mockedEvent);
    }

}
