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
package com.intuit.wasabi.repository.impl.cassandra;

import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.repository.AuditLogRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ApplicationNameSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.BucketLabelSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentIDSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.ExperimentLabelSerializer;
import com.intuit.wasabi.repository.impl.cassandra.serializer.UsernameSerializer;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.model.CqlResult;
import com.netflix.astyanax.model.Row;
import com.netflix.astyanax.model.Rows;
import com.netflix.astyanax.query.ColumnFamilyQuery;
import com.netflix.astyanax.query.CqlQuery;
import com.netflix.astyanax.query.PreparedCqlQuery;
import com.netflix.astyanax.serializers.DateSerializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;


/**
 * Tests for {@link CassandraAuditLogRepository}.
 */
@SuppressWarnings("unchecked")
public class CassandraAuditLogRepositoryTest {
    // the tested item
    private CassandraAuditLogRepository calr;

    // important mocks
    private CassandraDriver driver;
    // NOTE: prepQuery.execute() needs to be stubbed whenever needed!
    // Additionally prepQuery.with[...]Buffer needs to be stubbed if it is not the applicationName
    private PreparedCqlQuery<Application.Name, String> prepQuery;

    // helpers
    private final ExperimentsKeyspace keyspace = new ExperimentsKeyspaceImpl();
    private final Experiment.Label expLabel = Mockito.mock(Experiment.Label.class);
    private final Experiment.ID expID = Mockito.mock(Experiment.ID.class);
    private final Bucket.Label bucketLabel = Mockito.mock(Bucket.Label.class);
    private final Calendar time = Calendar.getInstance();
    private final String dummyStringValue = "DummyString";

    private Application.Name appName;
    private Rows<Application.Name, String> rows;

    @Before
    public void prepareComplexCALR() throws Exception {
        // init time and appName
        time.set(2000, Calendar.JUNE, 2, 13, 1, 2);
        appName = resetAppName();

        // mock driver and its calls
        driver = Mockito.mock(CassandraDriver.class);
        Keyspace driverKeyspace = Mockito.mock(Keyspace.class);
        Mockito.when(driver.getKeyspace()).thenReturn(driverKeyspace);

        ColumnFamilyQuery<Application.Name, String> cfq = Mockito.mock(ColumnFamilyQuery.class);
        Mockito.when(driverKeyspace.prepareQuery(keyspace.auditlogCF())).thenReturn(cfq);

        CqlQuery<Application.Name, String> cqlQuery = Mockito.mock(CqlQuery.class);
        Mockito.when(cfq.withCql(Mockito.anyString())).thenReturn(cqlQuery);

        prepQuery = Mockito.mock(PreparedCqlQuery.class);
        Mockito.when(cqlQuery.asPreparedStatement()).thenReturn(prepQuery);
        Mockito.when(prepQuery.withByteBufferValue(appName, ApplicationNameSerializer.get())).thenReturn(prepQuery);
        Mockito.when(prepQuery.withByteBufferValue(AuditLogRepository.GLOBAL_ENTRY_APPLICATION, ApplicationNameSerializer.get())).thenReturn(prepQuery);
        // NOTE: prepQuery.execute needs to be stubbed whenever needed!

        // create real CALR
        calr = new CassandraAuditLogRepository(driver, keyspace);

        // rows as simplified results
        rows = Mockito.mock(Rows.class);

        ColumnList<String> cl = createMockedALEColumnList();
        Row<Application.Name, String> row = Mockito.mock(Row.class);
        Iterator<Row<Application.Name, String>> iter = Mockito.mock(Iterator.class);
        Mockito.when(rows.iterator()).thenReturn(iter);
        Mockito.when(rows.iterator().hasNext()).thenReturn(true).thenReturn(false);
        Mockito.when(rows.iterator().next()).thenReturn(row);
        Mockito.when(row.getKey()).thenReturn(null);
        Mockito.when(row.getColumns()).thenReturn(cl);
    }

    @Test
    public void testGetCompleteAuditLogEntryList() throws Exception {
        OperationResult<CqlResult<Application.Name, String>> opResult = Mockito.mock(OperationResult.class);
        Mockito.when(prepQuery.execute()).thenReturn(opResult);
        CqlResult<Application.Name, String> cqlResult = Mockito.mock(CqlResult.class);
        Mockito.when(opResult.getResult()).thenReturn(cqlResult);
        Mockito.when(cqlResult.getRows()).thenReturn(rows);

        Assert.assertEquals(1, calr.getCompleteAuditLogEntryList().size());
    }

    @Test
    public void testGetCompleteAuditLogEntryListLimit() throws Exception {
        OperationResult<CqlResult<Application.Name, String>> opResult = Mockito.mock(OperationResult.class);
        Mockito.when(prepQuery.execute()).thenReturn(opResult);
        CqlResult<Application.Name, String> cqlResult = Mockito.mock(CqlResult.class);
        Mockito.when(opResult.getResult()).thenReturn(cqlResult);
        Mockito.when(cqlResult.getRows()).thenReturn(rows);

        Assert.assertEquals(1, calr.getCompleteAuditLogEntryList(10).size());
    }

    @Test
    public void testGetAuditLogEntryList() throws Exception {
        OperationResult<CqlResult<Application.Name, String>> opResult = Mockito.mock(OperationResult.class);
        Mockito.when(prepQuery.execute()).thenReturn(opResult);
        CqlResult<Application.Name, String> cqlResult = Mockito.mock(CqlResult.class);
        Mockito.when(opResult.getResult()).thenReturn(cqlResult);
        Mockito.when(cqlResult.getRows()).thenReturn(rows);

        Assert.assertEquals(1, calr.getAuditLogEntryList(appName).size());
    }

    @Test
    public void testGetAuditLogEntryListLimit() throws Exception {
        OperationResult<CqlResult<Application.Name, String>> opResult = Mockito.mock(OperationResult.class);
        Mockito.when(prepQuery.execute()).thenReturn(opResult);
        CqlResult<Application.Name, String> cqlResult = Mockito.mock(CqlResult.class);
        Mockito.when(opResult.getResult()).thenReturn(cqlResult);
        Mockito.when(cqlResult.getRows()).thenReturn(rows);

        appName = AuditLogRepository.GLOBAL_ENTRY_APPLICATION;
        Assert.assertEquals(1, calr.getAuditLogEntryList(appName, 10).size());
    }

    @Test
    public void testGetGlobalAuditLogEntryList() throws Exception {
        OperationResult<CqlResult<Application.Name, String>> opResult = Mockito.mock(OperationResult.class);
        Mockito.when(prepQuery.execute()).thenReturn(opResult);
        CqlResult<Application.Name, String> cqlResult = Mockito.mock(CqlResult.class);
        Mockito.when(opResult.getResult()).thenReturn(cqlResult);
        Mockito.when(cqlResult.getRows()).thenReturn(rows);

        appName = AuditLogRepository.GLOBAL_ENTRY_APPLICATION;
        Assert.assertEquals(1, calr.getGlobalAuditLogEntryList().size());
    }

    @Test
    public void testGetGlobalAuditLogEntryListLimit() throws Exception {
        OperationResult<CqlResult<Application.Name, String>> opResult = Mockito.mock(OperationResult.class);
        Mockito.when(prepQuery.execute()).thenReturn(opResult);
        CqlResult<Application.Name, String> cqlResult = Mockito.mock(CqlResult.class);
        Mockito.when(opResult.getResult()).thenReturn(cqlResult);
        Mockito.when(cqlResult.getRows()).thenReturn(rows);

        Assert.assertEquals(1, calr.getGlobalAuditLogEntryList(10).size());
    }

    @Test
    public void testStoreEntry() throws Exception {
        Mockito.when(prepQuery.withByteBufferValue(time.getTime(), DateSerializer.get())).thenReturn(prepQuery);
        Mockito.when(prepQuery.withByteBufferValue(EventLog.SYSTEM_USER.getUsername(), UsernameSerializer.get())).thenReturn(prepQuery);
        Mockito.when(prepQuery.withByteBufferValue(expID, ExperimentIDSerializer.get())).thenReturn(prepQuery);
        Mockito.when(prepQuery.withByteBufferValue(expLabel, ExperimentLabelSerializer.get())).thenReturn(prepQuery);
        Mockito.when(prepQuery.withByteBufferValue(bucketLabel, BucketLabelSerializer.get())).thenReturn(prepQuery);
        Mockito.when(prepQuery.withStringValue(Mockito.anyString())).thenReturn(prepQuery);

        // store entry successfully
        AuditLogEntry ale = new AuditLogEntry(time, EventLog.SYSTEM_USER, AuditLogAction.UNSPECIFIED_ACTION, appName, expLabel, expID, bucketLabel, dummyStringValue, dummyStringValue, dummyStringValue);
        Mockito.when(prepQuery.execute()).thenReturn(null);
        Assert.assertTrue(calr.storeEntry(ale));

        // store entry with many null values successfully
        ale = new AuditLogEntry(time, EventLog.SYSTEM_USER, AuditLogAction.UNSPECIFIED_ACTION);
        Mockito.when(prepQuery.execute()).thenReturn(null);
        Assert.assertTrue(calr.storeEntry(ale));

        // fail storing
        Mockito.when(prepQuery.execute()).thenThrow(Mockito.mock(ConnectionException.class));
        Assert.assertFalse(calr.storeEntry(ale));

        // fail storing "null" entry
        try {
            calr.storeEntry(null);
            Assert.fail();
        } catch (RepositoryException ignored) {
        }
    }

    /**
     * Tests {@link CassandraAuditLogRepository#cqlWithApplication(String, Application.Name)}
     * and {@link CassandraAuditLogRepository#cqlSelectAll(String)}
     * for thrown exceptions.
     */
    @Test
    public void testCqlSelectConnectionExceptions() throws Exception {
        // prepare mocks
        Mockito.when(prepQuery.execute()).thenThrow(Mockito.mock(ConnectionException.class));

        try {
            calr.cqlSelectAll("SELECT * FROM auditlog;");
            Assert.fail();
        } catch (RepositoryException ignored) {
        }

        try {
            calr.cqlWithApplication("SELECT * FROM auditlog;", appName);
            Assert.fail();
        } catch (RepositoryException ignored) {
        }
    }

    @Test
    public void testReadAuditLogEntryList() throws Exception {
        // call method
        List<AuditLogEntry> entries = calr.readAuditLogEntryList(appName, rows);

        // verify results
        Assert.assertEquals(1, entries.size());
    }

    @Test
    public void testReadAuditLogEntry() throws Exception {
        // prepare parameter mocks
        ColumnList<String> cl = createMockedALEColumnList();

        // call method
        AuditLogEntry ale = calr.readAuditLogEntry(appName, cl);

        // verify results
        Assert.assertEquals(time, ale.getTime());
        Assert.assertEquals(AuditLogAction.UNSPECIFIED_ACTION, ale.getAction());
        Assert.assertEquals(appName, ale.getApplicationName());
        Assert.assertEquals(expLabel, ale.getExperimentLabel());
        Assert.assertEquals(expID, ale.getExperimentId());
        Assert.assertEquals(bucketLabel, ale.getBucketLabel());
        Assert.assertEquals(dummyStringValue, ale.getChangedProperty());
        Assert.assertEquals(dummyStringValue, ale.getBefore());
        Assert.assertEquals(dummyStringValue, ale.getAfter());
        Assert.assertEquals(dummyStringValue, ale.getUser().getFirstName());
        Assert.assertEquals(dummyStringValue, ale.getUser().getLastName());
        Assert.assertEquals(dummyStringValue, ale.getUser().getUserId());
        Assert.assertEquals(dummyStringValue, ale.getUser().getEmail());
        Assert.assertEquals(dummyStringValue.toLowerCase(), ale.getUser().getUsername().toString());

        Mockito.when(cl.getDateValue("time", null)).thenReturn(null);
        ale = calr.readAuditLogEntry(appName, cl);
        Assert.assertNull(ale);

        ale = calr.readAuditLogEntry(appName, null);
        Assert.assertNull(ale);

        Mockito.when(cl.getValue("bucket_label", BucketLabelSerializer.get(), null)).thenThrow(IllegalArgumentException.class);
        ale = calr.readAuditLogEntry(appName, cl);
        Assert.assertNull(ale);
    }

    private ColumnList<String> createMockedALEColumnList() {
        ColumnList<String> cl = Mockito.mock(ColumnList.class);

        Mockito.when(cl.getStringValue(Mockito.anyString(), Mockito.eq(""))).thenReturn(dummyStringValue);
        Mockito.when(cl.getStringValue(Mockito.eq("action"), Mockito.anyString())).thenReturn(dummyStringValue);
        Mockito.when(cl.getDateValue("time", null)).thenReturn(time.getTime());
        Mockito.when(cl.getValue("experiment_label", ExperimentLabelSerializer.get(), null)).thenReturn(expLabel);
        Mockito.when(cl.getValue("experiment_id", ExperimentIDSerializer.get(), null)).thenReturn(expID);
        Mockito.when(cl.getValue("bucket_label", BucketLabelSerializer.get(), null)).thenReturn(bucketLabel);

        return cl;
    }

    private Application.Name resetAppName() {
        return Application.Name.valueOf("AppName");
    }
}
