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
package com.intuit.wasabi.email.impl;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.email.EmailLinksList;
import com.intuit.wasabi.email.EmailTextProcessor;
import com.intuit.wasabi.experimentobjects.Application;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests the {@link NoopEmailImpl} dummy implementation.
 */
public class NoopEmailImplTest {

    @Test
    public void testNoop() {
        EmailTextProcessor etp = Mockito.mock(EmailTextProcessor.class);
        NoopEmailImpl noop = new NoopEmailImpl();
        List<String> links = new ArrayList<>();
        links.add("https://foo.bar.com/");

        noop.setEmailTextProcessor(etp);
        noop.sendEmailForUserPermission(Application.Name.valueOf("appName"), UserInfo.Username.valueOf("user"), EmailLinksList.withEmailLinksList(links).build());

        noop.setEmailTextProcessor(null);
        noop.sendEmailForUserPermission(Application.Name.valueOf("appName"), UserInfo.Username.valueOf("user"), EmailLinksList.withEmailLinksList(links).build());

        noop.doSend("Unit - Test Subject", "nothing to worry", "test-user@example.com");

        Assert.assertTrue(noop.isActive());
        noop.setActive(false);
        Assert.assertTrue(noop.isActive());
    }

}
