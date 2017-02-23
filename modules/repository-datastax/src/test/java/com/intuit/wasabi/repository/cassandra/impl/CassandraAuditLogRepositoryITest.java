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
package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.mapping.Mapper;
import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AuditLogRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.IntegrationTestBase;
import com.intuit.wasabi.repository.cassandra.accessor.audit.AuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.audit.AuditLog;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CassandraAuditLogRepositoryITest extends IntegrationTestBase {

    AuditLogAccessor auditLogAccessor;

    List<AuditLogEntry> result;

    CassandraAuditLogRepository repository;

    private AuditLogEntry entry;
    private AuditLogEntry entryWithMissingApplicationName;

    private Mapper<AuditLog> mapper;

    private String applicationName;

    private UserInfo userInfo;

    private AuditLogEntry entry2;

    @Before
    public void setUp() throws Exception {
        IntegrationTestBase.setup();

        if (repository != null) return;

        mapper = manager.mapper(AuditLog.class);
        auditLogAccessor = manager.createAccessor(AuditLogAccessor.class);
        applicationName = "ApplicationName_" + System.currentTimeMillis();
        session.execute("delete from wasabi_experiments.auditlog where application_name = '"
                + applicationName + "'");

        session.execute("delete from wasabi_experiments.auditlog where application_name = '"
                + AuditLogRepository.GLOBAL_ENTRY_APPLICATION.toString() + "'");
        userInfo = UserInfo.newInstance(Username.valueOf("username1")).withEmail("email1")
                .withUserId("userid1").withFirstName("fn").withLastName("ln").build();

        repository = new CassandraAuditLogRepository(auditLogAccessor);
        entry =
                new AuditLogEntry(Calendar.getInstance(),
                        userInfo, AuditLogAction.BUCKET_CHANGED,
                        Application.Name.valueOf(applicationName),
                        Experiment.Label.valueOf("l1"),
                        Experiment.ID.newInstance(),
                        Bucket.Label.valueOf("b1"),
                        "prop1", "v1", "v2");

        entry2 =
                new AuditLogEntry(Calendar.getInstance(),
                        userInfo, AuditLogAction.BUCKET_CHANGED,
                        Application.Name.valueOf(applicationName),
                        Experiment.Label.valueOf("l1"),
                        Experiment.ID.newInstance(),
                        Bucket.Label.valueOf("b1"),
                        "prop1", "v1", "v2");

        entryWithMissingApplicationName =
                new AuditLogEntry(Calendar.getInstance(),
                        userInfo, AuditLogAction.BUCKET_CHANGED,
                        null,
                        Experiment.Label.valueOf("l1"),
                        Experiment.ID.newInstance(),
                        Bucket.Label.valueOf("b1"),
                        "prop1", "v1", "v2");
    }

    @Test
    public void testSaveAndGetEntrySuccess() {
        boolean success = repository.storeEntry(entry);
        assertEquals("value should be same", true, success);
        result = repository.getAuditLogEntryList(entry.getApplicationName());

        assertEquals("Value should be same", 1, result.size());
        assertEquals("Values shouild be same", entry.toString(),
                result.get(0).toString());

    }

    @Test
    public void testSaveOneAndGetEntryWithLimitSuccess() {
        boolean success = repository.storeEntry(entry);
        assertEquals("value should be same", true, success);
        result = repository.getAuditLogEntryList(entry.getApplicationName(), 5);

        assertEquals("Value should be same", 1, result.size());
        assertEquals("Values shouild be same", entry.toString(),
                result.get(0).toString());

    }

    @Test
    public void testSave2AndGetOneEntryWithLimitSuccess() {
        boolean success = repository.storeEntry(entry);
        assertEquals("value should be same", true, success);


        success = repository.storeEntry(entry2);
        assertEquals("value should be same", true, success);

        result = repository.getAuditLogEntryList(entry.getApplicationName(), 5);

        assertEquals("Value should be same", 2, result.size());

        result = repository.getAuditLogEntryList(entry.getApplicationName(), 1);

        assertEquals("Value should be same", 1, result.size());

    }

    @Test
    public void testSaveAndGetEntryWithEmptyUserExceptUsernameInfoSuccess() {
        // The reason for setting other attribs to "" is because the repository
        // code uses empty string. So to make the comparison by string , it is simpler
        // to make them empty strings
        UserInfo userInfo = UserInfo.newInstance(Username.valueOf("u1"))
                .withEmail("").withFirstName("").withLastName("").withUserId("").build();
        entry =
                new AuditLogEntry(Calendar.getInstance(),
                        userInfo, AuditLogAction.BUCKET_CHANGED,
                        Application.Name.valueOf(applicationName),
                        Experiment.Label.valueOf("l1"),
                        Experiment.ID.newInstance(),
                        Bucket.Label.valueOf("b1"),
                        "prop1", "v1", "v2");
        boolean success = repository.storeEntry(entry);
        assertEquals("value should be same", true, success);
        result = repository.getAuditLogEntryList(entry.getApplicationName());

        assertEquals("Value should be same", 1, result.size());
        assertEquals("Values shouild be same", entry.toString(),
                result.get(0).toString());

    }

    @Test
    public void testSaveAndGetEntryWithEmptyPropertySuccess() {
        // The reason for setting other attribs to "" is because the repository
        // code uses empty string. So to make the comparison by string , it is simpler
        // to make them empty strings
        UserInfo userInfo = UserInfo.newInstance(Username.valueOf("u1"))
                .withEmail("").withFirstName("").withLastName("").withUserId("").build();
        entry =
                new AuditLogEntry(Calendar.getInstance(),
                        userInfo, AuditLogAction.BUCKET_CHANGED,
                        Application.Name.valueOf(applicationName),
                        Experiment.Label.valueOf("l1"),
                        Experiment.ID.newInstance(),
                        Bucket.Label.valueOf("b1"),
                        "", "", "");
        boolean success = repository.storeEntry(entry);
        assertEquals("value should be same", true, success);
        result = repository.getAuditLogEntryList(entry.getApplicationName());

        assertEquals("Value should be same", 1, result.size());
        assertEquals("Values shouild be same", entry.toString(),
                result.get(0).toString());
    }

    @Test
    public void testSaveWithMissingApplicationNameWithAndWithoutLimitsSuccess() {
        boolean success = repository.storeEntry(entryWithMissingApplicationName);
        assertEquals("value should be same", true, success);
        result = repository.getAuditLogEntryList(AuditLogRepository.GLOBAL_ENTRY_APPLICATION);

        assertEquals("Value should be same", 1, result.size());
        assertEquals("Values shouild be same", entryWithMissingApplicationName.toString(),
                result.get(0).toString());

        result = repository.getGlobalAuditLogEntryList();

        assertEquals("Value should be same", 1, result.size());
        assertEquals("Values shouild be same", entryWithMissingApplicationName.toString(),
                result.get(0).toString());

        result = repository.getGlobalAuditLogEntryList(5);

        assertEquals("Value should be same", 1, result.size());
        assertEquals("Values shouild be same", entryWithMissingApplicationName.toString(),
                result.get(0).toString());
    }

    @Test
    public void testGetCompletedLogsWithAndWithoutLimitsSuccess() {

        result = repository.getCompleteAuditLogEntryList();

        assertTrue("Value should be greater than 1", result.size() >= 1);

        result = repository.getCompleteAuditLogEntryList(1);

        assertTrue("Value should be greater than 1", result.size() == 1);

        result = repository.getCompleteAuditLogEntryList(5);

        assertTrue("Value should be max 5", result.size() <= 5);
    }


    @Test(expected = RepositoryException.class)
    public void testNullEntryThrowsRepositoryException() {
        repository.storeEntry(null);
    }

}
