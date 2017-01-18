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
package com.intuit.wasabi.api.pagination.comparators.impl;

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
 * Tests for {@link AuditLogEntryComparator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AuditLogEntryComparatorTest {
    private AuditLogEntry auditLogEntry1;
    private AuditLogEntry auditLogEntry2;

    @Mock
    private Experiment experiment;

    @Before
    public void setup() {
        Experiment.ID experimentID = Experiment.ID.newInstance();
        Mockito.doReturn(experimentID).when(experiment).getID();
        Experiment.Label experimentLabel = Experiment.Label.valueOf("RedButtonExperience");
        Mockito.doReturn(experimentLabel).when(experiment).getLabel();

        auditLogEntry1 = new AuditLogEntry(
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

        Calendar secondDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        secondDate.add(Calendar.YEAR, 1);
        auditLogEntry2 = new AuditLogEntry(
                secondDate,
                UserInfo.from(UserInfo.Username.valueOf("jane"))
                        .withUserId("jadoe")
                        .withEmail("jadoe@example.com")
                        .withFirstName("Jane")
                        .withLastName("Doe")
                        .build(),
                AuditLogAction.EXPERIMENT_CREATED,
                experiment,
                null,
                "created",
                null,
                "Experiment new!");
    }

    @Test
    public void testCompare() throws Exception {
        AuditLogEntryComparator auditLogEntryComparator = new AuditLogEntryComparator();

        HashMap<String, Integer> testCases = new HashMap<>();
        testCases.put("firstname", 1);
        testCases.put("lastname", 0);
        testCases.put("user", 1);
        testCases.put("username", 1);
        testCases.put("userid", 1);
        testCases.put("mail", 1);
        testCases.put("action", -1);
        testCases.put("experiment", 0);
        testCases.put("bucket", -1);
        testCases.put("app", 0);
        testCases.put("time", -1);
        testCases.put("attribute", -1);
        testCases.put("before", -1);
        testCases.put("after", -1);
        testCases.put("description", -1);

        for (Map.Entry<String, Integer> testCase : testCases.entrySet()) {
            auditLogEntryComparator.replaceSortorder(testCase.getKey());

            int magnitudeResult = auditLogEntryComparator.compare(auditLogEntry1, auditLogEntry2);
            if (magnitudeResult != 0) {
                magnitudeResult /= Math.abs(magnitudeResult);
            }
            Assert.assertEquals("test case " + testCase.getKey() + " failed.",
                    testCase.getValue().intValue(),
                    magnitudeResult);
        }
    }
}
