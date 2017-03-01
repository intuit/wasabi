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
package com.intuit.wasabi.api.pagination.filters.impl;

import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * Tests for {@link AuditLogEntryFilter}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuditLogEntryFilterTest {
    private AuditLogEntry auditLogEntry;

    @Mock
    private Experiment experiment;

    @Before
    public void setup() {
        Experiment.ID experimentID = Experiment.ID.newInstance();
        Mockito.doReturn(experimentID).when(experiment).getID();
        Experiment.Label experimentLabel = Experiment.Label.valueOf("RedButtonExperience");
        Mockito.doReturn(experimentLabel).when(experiment).getLabel();

        auditLogEntry = new AuditLogEntry(
                Calendar.getInstance(TimeZone.getTimeZone("GMT")),
                UserInfo.from(UserInfo.Username.valueOf("john"))
                        .withUserId("jodoe")
                        .withEmail("jodoe@example.com")
                        .withFirstName("John")
                        .withLastName("Doe")
                        .build(),
                AuditLogAction.BUCKET_CHANGED,
                experiment,
                Bucket.Label.valueOf("A"),
                "allocation",
                "100",
                "50");
    }

    @Test
    public void testTest() throws Exception {
        AuditLogEntryFilter auditLogEntryFilter = new AuditLogEntryFilter();

        HashMap<String, Boolean> testCases = new HashMap<>();
        testCases.put("firstname=john", true);
        testCases.put("lastname=duck", false);
        testCases.put("user=oh", true);
        testCases.put("username=oh", true);
        testCases.put("userid=doe", true);
        testCases.put("mail=wasabi@example.com", false);
        testCases.put("action=experiment", false);
        testCases.put("experiment=RedButton", true);
        testCases.put("bucket=A", true);
        testCases.put("app=testApp", false);
        testCases.put("time=Summer", false);
        testCases.put("attribute=alloc", true);
        testCases.put("before=100", true);
        testCases.put("after=new value", false);
        testCases.put("description=some description", false);
        testCases.put("john", true);
        testCases.put("john,experiment=Experience", true);

        for (Map.Entry<String, Boolean> testCase : testCases.entrySet()) {
            auditLogEntryFilter.replaceFilter(testCase.getKey(), "+0000");

            Assert.assertEquals("test case " + testCase.getKey() + " failed.",
                    testCase.getValue(),
                    auditLogEntryFilter.test(auditLogEntry));
        }
    }
}
