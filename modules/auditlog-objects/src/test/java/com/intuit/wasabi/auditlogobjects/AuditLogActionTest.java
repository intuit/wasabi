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
package com.intuit.wasabi.auditlogobjects;

import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.eventlog.events.AuthorizationChangeEvent;
import com.intuit.wasabi.eventlog.events.BucketChangeEvent;
import com.intuit.wasabi.eventlog.events.BucketCreateEvent;
import com.intuit.wasabi.eventlog.events.BucketDeleteEvent;
import com.intuit.wasabi.eventlog.events.EventLogEvent;
import com.intuit.wasabi.eventlog.events.ExperimentChangeEvent;
import com.intuit.wasabi.eventlog.events.ExperimentCreateEvent;
import com.intuit.wasabi.eventlog.events.SimpleEvent;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tests for {@link AuditLogAction}.
 */
public class AuditLogActionTest {

    @Mock
    private AuditLogEntry entry;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(entry.getAfter()).thenReturn("AFTER_VALUE");
        Mockito.when(entry.getBefore()).thenReturn("BEFORE_VALUE");
        Application.Name appName = Mockito.mock(Application.Name.class);
        Mockito.when(entry.getApplicationName()).thenReturn(appName);
        Mockito.when(entry.getChangedProperty()).thenReturn("PROPERTY");
        Mockito.when(entry.getBucketLabel()).thenReturn(Mockito.mock(Bucket.Label.class));

    }

    @Test
    public void testGetActionForEvent() throws Exception {
        Assert.assertEquals(AuditLogAction.UNSPECIFIED_ACTION, AuditLogAction.getActionForEvent(new SimpleEvent("SimpleEvent")));
        ExperimentCreateEvent expCreateEvt = new ExperimentCreateEvent(Mockito.mock(Experiment.class));
        Assert.assertEquals(AuditLogAction.EXPERIMENT_CREATED, AuditLogAction.getActionForEvent(expCreateEvt));
    }

    @Test
    public void testGetDescription() throws Exception {
        Mockito.when(entry.getAction()).thenReturn(null, AuditLogAction.EXPERIMENT_CREATED, AuditLogAction.EXPERIMENT_CREATED, AuditLogAction.EXPERIMENT_CHANGED, AuditLogAction.EXPERIMENT_CHANGED, AuditLogAction.BUCKET_CREATED, AuditLogAction.BUCKET_CREATED, AuditLogAction.BUCKET_CHANGED, AuditLogAction.BUCKET_CHANGED);
        for (int i = 0; i < AuditLogAction.values().length + 1; ++i) {
            Assert.assertNotNull(AuditLogAction.getDescription(entry));
        }
        Assert.assertNotNull(AuditLogAction.getDescription(null));
    }

    @Test
    public void testGetDescriptionTexts() throws Exception {
        Map<EventLogEvent, String[]> eventKeywords = new HashMap<>();

        eventKeywords.put(new SimpleEvent("Simple Event"), new String[]{""});

        Experiment exp = Experiment.withID(Experiment.ID.valueOf(new UUID(1, 1)))
                .withApplicationName(Application.Name.valueOf("ABCDEF"))
                .withLabel(Experiment.Label.valueOf("FEDCBA")).build();
        eventKeywords.put(new ExperimentCreateEvent(exp), new String[]{"created", "experiment"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "state", "", "DRAFT"), new String[]{"draft", "experiment"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "state", "", "RUNNING"), new String[]{"started", "experiment"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "state", "", "PAUSED"), new String[]{"stopped", "experiment"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "state", "", "TERMINATED"), new String[]{"terminated", "experiment"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "state", "", "DELETED"), new String[]{"deleted", "experiment"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "description", "", "sometext"), new String[]{"description", "changed"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "sampling_percent", "0.2", "0.6"), new String[]{"60%", "sampling"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "start_time", "", "2014-03-12"), new String[]{"start time", "2014-03-12"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "end_time", "", "2014-03-12"), new String[]{"end time", "2014-03-12"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "isPersonalizationEnabled", "", "true"), new String[]{"enabled", "personalization"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "isPersonalizationEnabled", "", "false"), new String[]{"disabled", "personalization"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "modelName", "", null), new String[]{"model", "name", "removed"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "modelName", "", "wasabi213"), new String[]{"model", "name", "wasabi213"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "modelVersion", "", null), new String[]{"model", "version", "removed"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "modelVersion", "", "1.32"), new String[]{"model", "version", "1.32"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "isRapidExperiment", "", "true"), new String[]{"enabled", "rapid", "experiment"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "isRapidExperiment", "", "false"), new String[]{"disabled", "rapid", "experiment"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "userCap", "2000", "1500"), new String[]{"changed", "rapid", "experiment", "1500"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "label", "FEDCBA", "ABCDEF"), new String[]{"label", "ABCDEF", "changed"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "applicationName", "", ""), new String[]{"application", "ABCDEF", "moved"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "rule", "", "salary < 2000"), new String[]{"rule", "changed", "salary < 2000"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "pages", "taxseason", null), new String[]{"page", "removed"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "pages", "", "taxseason"), new String[]{"pages", "set"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "pages", "all pages", null), new String[]{"pages", "removed", "all"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "mutex", "another", null), new String[]{"mutual", "exclusion", "removed", "another"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "mutex", null, "another"), new String[]{"mutual", "exclusion", "added", "another"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "mutex", null, null), new String[]{"mutual", "exclusion", "modified"});
        eventKeywords.put(new ExperimentChangeEvent(exp, "creatorID", null, "someone"), new String[]{"changed", "experiment"});

        Bucket bucket = new Bucket();
        bucket.setLabel(Bucket.Label.valueOf("red"));
        bucket.setAllocationPercent(0.5);
        eventKeywords.put(new BucketCreateEvent(exp, bucket), new String[]{"created", "bucket", "red", "allocation", "50%"});
        eventKeywords.put(new BucketDeleteEvent(exp, bucket), new String[]{"deleted", "bucket", "red"});
        eventKeywords.put(new BucketChangeEvent(exp, bucket, "allocation", ".4", ".3"), new String[]{"changed", "bucket", "red", "allocation", "30%"});
        eventKeywords.put(new BucketChangeEvent(exp, bucket, "is_control", "", "true"), new String[]{"bucket", "red", "control", "assigned"});
        eventKeywords.put(new BucketChangeEvent(exp, bucket, "is_control", "", "false"), new String[]{"bucket", "red", "control", "removed"});
        eventKeywords.put(new BucketChangeEvent(exp, bucket, "description", "", "new"), new String[]{"added", "bucket", "red", "description", "new"});
        eventKeywords.put(new BucketChangeEvent(exp, bucket, "description", "before", "after"), new String[]{"changed", "bucket", "red", "description", "after"});
        eventKeywords.put(new BucketChangeEvent(exp, bucket, "description", "", ""), new String[]{"removed", "bucket", "red", "description"});
        eventKeywords.put(new BucketChangeEvent(exp, bucket, "payload", "", ""), new String[]{"removed", "bucket", "red", "payload"});
        eventKeywords.put(new BucketChangeEvent(exp, bucket, "payload", "", "additions"), new String[]{"changed", "bucket", "red", "payload", "additions"});
        eventKeywords.put(new BucketChangeEvent(exp, bucket, "something", "", ""), new String[]{"changed", "bucket", "red"});
        eventKeywords.put(new BucketChangeEvent(exp, bucket, "state", "OPEN", "CLOSED"), new String[]{"bucket", "red", "closed"});
        eventKeywords.put(new BucketChangeEvent(exp, bucket, "state", "OPEN", "EMPTY"), new String[]{"changed", "bucket", "red", "state"});

        UserInfo admin = UserInfo.from(UserInfo.Username.valueOf("AdminUser"))
                .withEmail("admin@example.com")
                .withFirstName("firstname")
                .withLastName("lastname")
                .withUserId("AdminUser")
                .build();
        UserInfo user = UserInfo.from(UserInfo.Username.valueOf("AffectedUser"))
                .withEmail("user@you.org")
                .withFirstName("Norman")
                .withLastName("Normal")
                .withUserId("AffectedUser")
                .build();
        eventKeywords.put(new AuthorizationChangeEvent(admin, exp.getApplicationName(), user, "READONLY", "ADMIN"), new String[]{"changed", "access", "ADMIN", "Norman", "Normal", "READ", "affecteduser"});
        eventKeywords.put(new AuthorizationChangeEvent(admin, exp.getApplicationName(), user, "READONLY", "READWRITE"), new String[]{"changed", "access", "WRITE", "Norman", "Normal", "READ", "affecteduser"});
        eventKeywords.put(new AuthorizationChangeEvent(admin, exp.getApplicationName(), user, "READWRITE", "READONLY"), new String[]{"changed", "access", "WRITE", "Norman", "Normal", "READ", "affecteduser"});
        eventKeywords.put(new AuthorizationChangeEvent(admin, exp.getApplicationName(), user, "", "ADMIN"), new String[]{"granted", "access", "ADMIN", "Norman", "Normal", "affecteduser"});
        eventKeywords.put(new AuthorizationChangeEvent(admin, exp.getApplicationName(), user, null, "ADMIN"), new String[]{"granted", "access", "ADMIN", "Norman", "Normal", "affecteduser"});
        eventKeywords.put(new AuthorizationChangeEvent(admin, exp.getApplicationName(), user, null, ""), new String[]{"removed", "access", "Norman", "Normal", "affecteduser"});
        eventKeywords.put(new AuthorizationChangeEvent(admin, exp.getApplicationName(), user, null, null), new String[]{"removed", "access", "Norman", "Normal", "affecteduser"});
        eventKeywords.put(new AuthorizationChangeEvent(admin, exp.getApplicationName(), user, "ADMIN", null), new String[]{"removed", "access", "Norman", "Normal", "affecteduser"});

        for (Map.Entry<EventLogEvent, String[]> entry : eventKeywords.entrySet()) {
            String desc = AuditLogAction.getDescription(AuditLogEntryFactory.createFromEvent(entry.getKey()));
            for (String s : entry.getValue()) {
                Assert.assertTrue("Message for '" + entry.getKey().getDefaultDescription() + "' does not contain '" + s + "'.\nWas:\n\t" + desc, desc.contains(s));
            }
        }
    }
}
