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

import com.datastax.driver.mapping.Result;
import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.audit.AuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.audit.AuditLog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CassandraAuditLogRepositoryTest {

    @Mock
    AuditLogAccessor accessor;

    @Mock
    Result<com.intuit.wasabi.repository.cassandra.pojo.audit.AuditLog> mockResult;

    @Mock
    AuditLog mockAuditLog;

    CassandraAuditLogRepository repository;

    private UserInfo userInfo;

    private String applicationName;

    private AuditLogEntry entry;

    private List<AuditLog> dbEntries;

    private AuditLog dbEntry;

    @Before
    public void setUp() throws Exception {
        repository = new CassandraAuditLogRepository(accessor);

        applicationName = "ApplicationName_" + System.currentTimeMillis();

        userInfo = UserInfo.newInstance(Username.valueOf("username1")).withEmail("email1")
                .withUserId("userid1").withFirstName("fn").withLastName("ln").build();

        entry = new AuditLogEntry(Calendar.getInstance(),
                userInfo, AuditLogAction.BUCKET_CHANGED,
                Application.Name.valueOf(applicationName),
                Experiment.Label.valueOf("l1"),
                Experiment.ID.newInstance(),
                Bucket.Label.valueOf("b1"),
                "prop1", "v1", "v2");

        dbEntry = new AuditLog();
        dbEntry.setAction(AuditLogAction.AUTHORIZATION_CHANGE.name());
        dbEntry.setTime(new Date());
        dbEntry.setUsername("un1");
        dbEntry.setAppName(applicationName);

        dbEntries = new ArrayList<>();
        dbEntries.add(dbEntry);
    }

    @Test
    public void testAuditLogGetActionThrowsException() {
        doThrow(new RuntimeException("runtimeexcep")).when(mockAuditLog).getAction();
        when(mockAuditLog.getTime()).thenReturn(new Date());
        when(mockAuditLog.getAppName()).thenReturn("testApp");
        ArrayList<AuditLog> entries = new ArrayList<>();
        entries.add(mockAuditLog);
        List<AuditLogEntry> result = repository.makeAuditLogEntries(entries);
        assertEquals("Size should be eq", 1, result.size());
        assertEquals("value shoudl be eq", AuditLogAction.UNSPECIFIED_ACTION, result.get(0).getAction());
    }

    @Test
    public void testAuditLogGetBuckeLabelThrowsException() {
        doThrow(new RuntimeException("runtimeexcep")).when(mockAuditLog).getBucketLabel();
        when(mockAuditLog.getTime()).thenReturn(new Date());
        when(mockAuditLog.getAppName()).thenReturn("testApp");
        ArrayList<AuditLog> entries = new ArrayList<>();
        entries.add(mockAuditLog);
        List<AuditLogEntry> result = repository.makeAuditLogEntries(entries);
        assertEquals("Size should be eq", 1, result.size());
        assertEquals("value shoudl be eq", null, result.get(0).getBucketLabel());
    }

    @Test
    public void testAuditLogGetExperimentLabelThrowsException() {
        doThrow(new RuntimeException("runtimeexcep")).when(mockAuditLog).getExperimentLabel();
        when(mockAuditLog.getTime()).thenReturn(new Date());
        when(mockAuditLog.getAppName()).thenReturn("testApp");
        ArrayList<AuditLog> entries = new ArrayList<>();
        entries.add(mockAuditLog);
        List<AuditLogEntry> result = repository.makeAuditLogEntries(entries);
        assertEquals("Size should be eq", 1, result.size());
        assertEquals("value shoudl be eq", null, result.get(0).getExperimentLabel());
    }

    @Test
    public void testAuditLogGetExprimentIdThrowsException() {
        doThrow(new RuntimeException("runtimeexcep")).when(mockAuditLog).getExperimentId();
        when(mockAuditLog.getTime()).thenReturn(new Date());
        when(mockAuditLog.getAppName()).thenReturn("testApp");
        ArrayList<AuditLog> entries = new ArrayList<>();
        entries.add(mockAuditLog);
        List<AuditLogEntry> result = repository.makeAuditLogEntries(entries);
        assertEquals("Size should be eq", 1, result.size());
        assertEquals("value shoudl be eq", null, result.get(0).getExperimentId());
    }

    @Test
    public void testSaveAndGetEntrySuccess() {

        boolean success = repository.storeEntry(entry);

        assertEquals("value should be same", true, success);

        when(accessor.getAuditLogEntryList(entry.getApplicationName().toString()))
                .thenReturn(mockResult);
        when(mockResult.all()).thenReturn(dbEntries);

        List<AuditLogEntry> result = repository.getAuditLogEntryList(entry.getApplicationName());

        assertEquals("Value should be same", 1, result.size());
        assertEquals("Values should be same", dbEntry.getTime(), result.get(0).getTime().getTime());
        assertEquals("Values should be same", dbEntry.getAction(), result.get(0).getAction().name());

    }

    @Test(expected = RepositoryException.class)
    public void testGetEntryThrowsException() {

        when(accessor.getAuditLogEntryList(entry.getApplicationName().toString()))
                .thenThrow(new RuntimeException("testException"));

        List<AuditLogEntry> result = repository.getAuditLogEntryList(entry.getApplicationName());
    }

    @Test(expected = RepositoryException.class)
    public void testGetEntryWithLimitThrowsException() {

        when(accessor.getAuditLogEntryList(entry.getApplicationName().toString(), 2))
                .thenThrow(new RuntimeException("testException"));

        List<AuditLogEntry> result = repository.getAuditLogEntryList(entry.getApplicationName(), 2);
    }

    @Test
    public void testGetCompleteEntrySuccess() {

        when(accessor.getCompleteAuditLogEntryList())
                .thenReturn(mockResult);
        when(mockResult.all()).thenReturn(dbEntries);

        List<AuditLogEntry> result = repository.getCompleteAuditLogEntryList();

        assertEquals("Value should be same", 1, result.size());
        assertEquals("Values should be same", dbEntry.getTime(), result.get(0).getTime().getTime());
        assertEquals("Values should be same", dbEntry.getAction(), result.get(0).getAction().name());

    }

    @Test
    public void testCompleteEntryWithLimitSuccess() {

        when(accessor.getCompleteAuditLogEntryList(1))
                .thenReturn(mockResult);
        when(mockResult.all()).thenReturn(dbEntries);

        List<AuditLogEntry> result = repository.getCompleteAuditLogEntryList(1);

        assertEquals("Value should be same", 1, result.size());
        assertEquals("Values should be same", dbEntry.getTime(), result.get(0).getTime().getTime());
        assertEquals("Values should be same", dbEntry.getAction(), result.get(0).getAction().name());

    }

    @Test(expected = RepositoryException.class)
    public void testGetCompleteEntryThrowsException() {

        when(accessor.getCompleteAuditLogEntryList())
                .thenThrow(new RuntimeException("testException"));

        List<AuditLogEntry> result = repository.getCompleteAuditLogEntryList();
    }

    @Test(expected = RepositoryException.class)
    public void testGetCompleteEntryWithLimitThrowsException() {

        when(accessor.getCompleteAuditLogEntryList(4))
                .thenThrow(new RuntimeException("testException"));

        List<AuditLogEntry> result = repository.getCompleteAuditLogEntryList(4);
    }

    @Test(expected = RepositoryException.class)
    public void testGetGlobalAuditLogEntryThrowsException() {

        when(accessor.getAuditLogEntryList(Mockito.anyString()))
                .thenThrow(new RuntimeException("testException"));

        List<AuditLogEntry> result = repository.getGlobalAuditLogEntryList();
    }

    @Test(expected = RepositoryException.class)
    public void testGetGlobalAuditLogEntryWithLimitThrowsException() {

        when(accessor.getAuditLogEntryList(Mockito.anyString(), Mockito.eq(1)))
                .thenThrow(new RuntimeException("testException"));

        List<AuditLogEntry> result = repository.getGlobalAuditLogEntryList(1);
    }
}
