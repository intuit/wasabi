/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.impl.cassandra;

import com.googlecode.catchexception.apis.CatchExceptionBdd;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.assignmentobjects.Assignment;
import com.intuit.wasabi.assignmentobjects.User;
import com.intuit.wasabi.cassandra.CassandraDriver;
import com.intuit.wasabi.eventlog.EventLog;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.Label;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.repository.AssignmentsRepository;
import com.intuit.wasabi.repository.ExperimentRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.impl.cassandra.CassandraAssignmentsRepository;
import com.intuit.wasabi.repository.impl.cassandra.ExperimentsKeyspace;
import com.netflix.astyanax.Keyspace;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.connectionpool.exceptions.HostDownException;
import com.netflix.astyanax.model.*;
import com.netflix.astyanax.query.ColumnFamilyQuery;
import com.netflix.astyanax.query.CqlQuery;
import com.netflix.astyanax.query.PreparedCqlQuery;
import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.BDDAssertions.then;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.*;


@RunWith(MockitoJUnitRunner.class)
public class CassandraAssignmentsRepositoryTest2 {
    @Mock
    private ExperimentRepository cassandraRepository;
    @Mock
    private CassandraDriver cassandraDriver;
    @Mock
    private ExperimentsKeyspace experimentsKeysapce;
    
    @Mock
    private ColumnFamilyQuery<User.ID,String> query;
    @Mock
    private Keyspace keyspace;

    @Mock
    private AssignmentsRepository assignmentsRepository;
    @Mock
    private ExperimentRepository dbRepository;
    @Mock
    private EventLog eventLog;
    
    @Mock
	private CqlQuery<User.ID,String> cqlQueryUserIdString;
    
    @Mock
	private PreparedCqlQuery<User.ID,String> preparedCqlQueryUserIdStringUserIdString;
    
    @Mock
	private OperationResult<CqlResult<User.ID,String>> operationResultUserIdString;
    
    @Mock
	private CqlResult<User.ID,String> cqlResultUserIdString;
    
    @Mock
	private Rows<User.ID,String> rowsUserIdString;

    @Mock Row<User.ID,String> rowUserIdString;
    
    @Mock
	private CqlQuery<Experiment.ID,String> cqlQueryExperimentIdString;
    
    @Mock(answer= Answers.RETURNS_DEEP_STUBS)
	private PreparedCqlQuery<Experiment.ID,String> preparedCqlQueryExperimentIdString;
    
    @Mock
	private OperationResult<CqlResult<Experiment.ID,String>> operationResultExperimentIdString;
    
    @Mock
	private CqlResult<Experiment.ID,String> cqlResultExperimentIdString;
    
    @Mock
	private Rows<Experiment.ID,String> rowsExperimentIdString;

    @Mock Row<Experiment.ID,String> rowExperimentIdString;
    
    @Mock
	private ColumnList<String> columns;
    
    @Mock
	private ColumnFamilyQuery<Experiment.ID, String> queryExperimentIdString;
    
    @Mock
	private Column<String> value;
    
    @Mock
	private Experiment experiment;
    
    @Mock
	private Assignment assignment;
    
    @Mock
	private ColumnFamilyQuery<UUID, String> queryUUIDString;
    
    @Mock
	private CqlQuery<UUID, String> cqlQueryUUIDString;

    @Mock
    private PreparedCqlQuery<UUID, String> preparedCqlQueryUUIDString;
    
    @Mock
	private Label label;
    
    @Mock
	private Application.Name applicationName;
    
    @Mock
	private Context context;

    @Test
    public void getUserAssignmentSuccess() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<User.ID,String>>any())).willReturn(query);
        
		given(query.withCql(isA(String.class))).willReturn(cqlQueryUserIdString);
        given(cqlQueryUserIdString.asPreparedStatement()).willReturn(preparedCqlQueryUserIdStringUserIdString);
        given(preparedCqlQueryUserIdStringUserIdString.withByteBufferValue(isA(User.ID.class), isA(Serializer.class))).
        	willReturn(preparedCqlQueryUserIdStringUserIdString);
        given(preparedCqlQueryUserIdStringUserIdString.withByteBufferValue(Mockito.isA(Application.Name.class), 
        		isA(Serializer.class))).	
    		willReturn(preparedCqlQueryUserIdStringUserIdString);
        given(preparedCqlQueryUserIdStringUserIdString.withStringValue(isA(String.class))).willReturn(preparedCqlQueryUserIdStringUserIdString);
        given(preparedCqlQueryUserIdStringUserIdString.execute()).willReturn(operationResultUserIdString);
        given(operationResultUserIdString.getResult()).willReturn(cqlResultUserIdString);
        
        
        given(cqlResultUserIdString.getRows()).willReturn(rowsUserIdString);
        given(rowsUserIdString.isEmpty()).willReturn(true);
        User.ID userID = User.ID.valueOf("u1");
        Application.Name appLabel = Application.Name.valueOf("a1");
        Context context = Context.valueOf("c1");
        
        Set<ID> result = cassandraAssignmentsRepository.getUserAssignments(userID, appLabel, context);
        
        assertTrue(result.isEmpty());
    }

    @Test
    public void getUserAssignmentSuccessOneRow() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<User.ID,String>>any())).willReturn(query);
        
		given(query.withCql(isA(String.class))).willReturn(cqlQueryUserIdString);
        given(cqlQueryUserIdString.asPreparedStatement()).willReturn(preparedCqlQueryUserIdStringUserIdString);
        given(preparedCqlQueryUserIdStringUserIdString.withByteBufferValue(isA(User.ID.class), isA(Serializer.class))).
        	willReturn(preparedCqlQueryUserIdStringUserIdString);
        given(preparedCqlQueryUserIdStringUserIdString.withByteBufferValue(Mockito.isA(Application.Name.class), 
        		isA(Serializer.class))).	
    		willReturn(preparedCqlQueryUserIdStringUserIdString);
        given(preparedCqlQueryUserIdStringUserIdString.withStringValue(isA(String.class))).willReturn(preparedCqlQueryUserIdStringUserIdString);
        given(preparedCqlQueryUserIdStringUserIdString.execute()).willReturn(operationResultUserIdString);
        given(operationResultUserIdString.getResult()).willReturn(cqlResultUserIdString);
        given(cqlResultUserIdString.getRows()).willReturn(rowsUserIdString);
        given(rowsUserIdString.isEmpty()).willReturn(false).willReturn(true);
        given(rowsUserIdString.size()).willReturn(1);
        given(rowsUserIdString.getRowByIndex(0)).willReturn(rowUserIdString);
        given(rowUserIdString.getColumns()).willReturn(columns);
        given(columns.getStringValue("bucket", null)).willReturn("b1");
        UUID uuid = UUID.randomUUID();
        given(columns.getUUIDValue("experiment_id",null)).willReturn(uuid);
        User.ID userID = User.ID.valueOf("u1");
        Application.Name appLabel = Application.Name.valueOf("a1");
        Context context = Context.valueOf("c1");
        
        Set<ID> result = cassandraAssignmentsRepository.getUserAssignments(userID, appLabel, context);
        
        assertEquals(result.size(),1);
        
        BDDAssertions.then(result.iterator().next()).isEqualTo(Experiment.ID.valueOf(uuid));
    }

    @Test
    public void getBucketAssignmentCountOneRow() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(isA(Experiment.ID.class), 
        		isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.execute()).willReturn(operationResultExperimentIdString);
        given(operationResultExperimentIdString.getResult()).willReturn(cqlResultExperimentIdString);
        given(cqlResultExperimentIdString.getRows()).willReturn(rowsExperimentIdString);
        given(rowsExperimentIdString.isEmpty()).willReturn(false).willReturn(true);
        given(rowsExperimentIdString.size()).willReturn(1);
        given(rowsExperimentIdString.getRowByIndex(0)).willReturn(rowExperimentIdString);
        given(rowExperimentIdString.getColumns()).willReturn(columns);
        given(columns.getStringValue("bucket_label", null)).willReturn("bl2");
        
        given(columns.getColumnByName("bucket_assignment_count")).willReturn(value);
        given(value.getLongValue()).willReturn(1L);
        
         AssignmentCounts result = cassandraAssignmentsRepository.getBucketAssignmentCount(experiment);
        
        assertEquals(result.getAssignments().size(),1);
        
        BDDAssertions.then(result.getExperimentID()).isEqualTo(id);
        assertEquals(result.getTotalUsers().getBucketAssignments(),1);
        assertEquals(result.getTotalUsers().getNullAssignments(),0);
        then(result.getTotalUsers().getTotal()).isEqualTo(1);
    }

    @Test
    public void removeIndexUserToBucketSuccess() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(any(), isA(Serializer.class)) // user_id
                .withByteBufferValue(any(), isA(Serializer.class)) // experiment_id
                .withStringValue(any(String.class)) //context
                .withByteBufferValue(any(), isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.execute()).willReturn(null);
        
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        User.ID userId = User.ID.valueOf("userId");
        cassandraAssignmentsRepository.removeIndexUserToBucket(userId, id, context, label);
    }

    @Test
    public void removeIndexUserToBucketThrowsException() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(any(), isA(Serializer.class)) // user_id
                .withByteBufferValue(any(), isA(Serializer.class)) // experiment_id
                .withStringValue(any(String.class)) //context
                .withByteBufferValue(any(), isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.execute()).willThrow(new HostDownException("test"));
        
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        User.ID userId = User.ID.valueOf("userId");
        CatchExceptionBdd.when(cassandraAssignmentsRepository).removeIndexUserToBucket(userId, id, context, label);
        CatchExceptionBdd.thenThrown(RepositoryException.class);
    }

    @Test
    public void removeIndexUserToExperimentSuccess() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(any(), isA(Serializer.class)) // user_id
                .withByteBufferValue(any(), isA(Serializer.class)) // experiment_id
                .withStringValue(any(String.class)) //context
                .withByteBufferValue(any(), isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.execute()).willReturn(null);
        
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        User.ID userId = User.ID.valueOf("userId");
        cassandraAssignmentsRepository.removeIndexUserToExperiment(userId, id, context, applicationName);
    }

    @Test
    public void removeIndexExperimentsToUserSuccess() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(any(), isA(Serializer.class)) // user_id
                .withByteBufferValue(any(), isA(Serializer.class)) // experiment_id
                .withStringValue(any(String.class)) //context
                .withByteBufferValue(any(), isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.execute()).willReturn(null);
        
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        User.ID userId = User.ID.valueOf("userId");
        cassandraAssignmentsRepository.removeIndexExperimentsToUser(userId, id, context, applicationName);
    }

    @Test(expected=RepositoryException.class)
    public void removeIndexExperimentsToUserThrowsException() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(any(), isA(Serializer.class)) // user_id
                .withByteBufferValue(any(), isA(Serializer.class)) // experiment_id
                .withStringValue(any(String.class)) //context
                .withByteBufferValue(any(), isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.execute()).willThrow(new HostDownException("test"));
        
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        User.ID userId = User.ID.valueOf("userId");
        cassandraAssignmentsRepository.removeIndexExperimentsToUser(userId, id, context, applicationName);
    }

    @SuppressWarnings("deprecation")
	@Test(expected=RepositoryException.class)
    public void removeIndexUserToExperimentThrowsException() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(any(), isA(Serializer.class)) // user_id
                .withByteBufferValue(any(), isA(Serializer.class)) // experiment_id
                .withStringValue(any(String.class)) //context
                .withByteBufferValue(any(), isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.execute()).willThrow(new HostDownException(""));
        
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        User.ID userId = User.ID.valueOf("userId");
        cassandraAssignmentsRepository.removeIndexUserToExperiment(userId, id, context, applicationName);
    }

    @Test
    public void updateBucketAssignmentCountUp() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(isA(Experiment.ID.class), 
        		isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(isA(Bucket.Label.class), 
        		isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.execute()).willReturn(null);
        
        cassandraAssignmentsRepository.updateBucketAssignmentCount(experiment,assignment, true);
    }

    @Test
    public void pushAssignmentToStagingSuccess() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<UUID,String>>any())).willReturn(queryUUIDString);
        
		given(queryUUIDString.withCql(isA(String.class))).willReturn(cqlQueryUUIDString);
        given(cqlQueryUUIDString.asPreparedStatement()).willReturn(preparedCqlQueryUUIDString);
        given(preparedCqlQueryUUIDString.withByteBufferValue(isA(String.class), 
        		isA(Serializer.class))).
        	willReturn(preparedCqlQueryUUIDString);
        given(preparedCqlQueryUUIDString.execute()).willReturn(null);
        
        cassandraAssignmentsRepository.pushAssignmentToStaging("exc1", "data1");
    }

    @Test(expected=RepositoryException.class)
    public void pushAssignmentToStagingThrowsException() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<UUID,String>>any())).willReturn(queryUUIDString);
        
		given(queryUUIDString.withCql(isA(String.class))).willReturn(cqlQueryUUIDString);
        given(cqlQueryUUIDString.asPreparedStatement()).willReturn(preparedCqlQueryUUIDString);
        given(preparedCqlQueryUUIDString.withByteBufferValue(isA(String.class), 
        		isA(Serializer.class))).
        	willReturn(preparedCqlQueryUUIDString);
        given(preparedCqlQueryUUIDString.execute()).willThrow(new HostDownException("test"));
        
        cassandraAssignmentsRepository.pushAssignmentToStaging("exc1", "data1");
    }

    @Test
    public void updateBucketAssignmentCountDown() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(isA(Experiment.ID.class), 
        		isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(isA(Bucket.Label.class), 
        		isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.execute()).willReturn(null);
        
        cassandraAssignmentsRepository.updateBucketAssignmentCount(experiment,assignment, false);
    }

    @Test(expected=RepositoryException.class)
    public void updateBucketAssignmentCountDownThrowsException() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(isA(Experiment.ID.class), 
        		isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(isA(Bucket.Label.class), 
        		isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        HostDownException hde = new HostDownException("test");
        given(preparedCqlQueryExperimentIdString.execute()).willThrow(hde);
        
        cassandraAssignmentsRepository.updateBucketAssignmentCount(experiment,assignment, false);
    }

    @Test(expected=RepositoryException.class)
    public void getBucketAssignmentCountThrowsException() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(isA(Experiment.ID.class), 
        		isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        HostDownException hde = new HostDownException("test");
        given(preparedCqlQueryExperimentIdString.execute()).willThrow(hde);
        AssignmentCounts result = cassandraAssignmentsRepository.getBucketAssignmentCount(experiment);
    }

    @Test
    public void getBucketAssignmentCountOneRowBucketLabelNull() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(isA(Experiment.ID.class), 
        		isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.execute()).willReturn(operationResultExperimentIdString);
        given(operationResultExperimentIdString.getResult()).willReturn(cqlResultExperimentIdString);
        given(cqlResultExperimentIdString.getRows()).willReturn(rowsExperimentIdString);
        given(rowsExperimentIdString.isEmpty()).willReturn(false).willReturn(true);
        given(rowsExperimentIdString.size()).willReturn(1);
        given(rowsExperimentIdString.getRowByIndex(0)).willReturn(rowExperimentIdString);
        given(rowExperimentIdString.getColumns()).willReturn(columns);
        given(columns.getStringValue("bucket_label", null)).willReturn("NULL");
        
        given(columns.getColumnByName("bucket_assignment_count")).willReturn(value);
        given(value.getLongValue()).willReturn(1L);
        
         AssignmentCounts result = cassandraAssignmentsRepository.getBucketAssignmentCount(experiment);
        
        then(result.getAssignments()).hasSize(1);
       
        BDDAssertions.then(result.getExperimentID()).isEqualTo(id);
        then(result.getTotalUsers().getBucketAssignments()).isEqualTo(0);
        then(result.getTotalUsers().getNullAssignments()).isEqualTo(1);
        then(result.getTotalUsers().getTotal()).isEqualTo(1);
    }

    @Test
    public void getBucketAssignmentCountZeroRows() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<Experiment.ID,String>>any())).willReturn(queryExperimentIdString);
        
		given(queryExperimentIdString.withCql(isA(String.class))).willReturn(cqlQueryExperimentIdString);
        given(cqlQueryExperimentIdString.asPreparedStatement()).willReturn(preparedCqlQueryExperimentIdString);
        Experiment.ID id = Experiment.ID.newInstance();
        given(experiment.getID()).willReturn(id);
        given(preparedCqlQueryExperimentIdString.withByteBufferValue(isA(Experiment.ID.class), 
        		isA(Serializer.class))).
        	willReturn(preparedCqlQueryExperimentIdString);
        given(preparedCqlQueryExperimentIdString.execute()).willReturn(operationResultExperimentIdString);
        given(operationResultExperimentIdString.getResult()).willReturn(cqlResultExperimentIdString);
        given(cqlResultExperimentIdString.getRows()).willReturn(rowsExperimentIdString);
        given(rowsExperimentIdString.isEmpty()).willReturn(true);
        
         AssignmentCounts result = cassandraAssignmentsRepository.getBucketAssignmentCount(experiment);
        
        then(result.getAssignments()).hasSize(1);
        then(result.getTotalUsers().getBucketAssignments()).isEqualTo(0);
        then(result.getTotalUsers().getNullAssignments()).isEqualTo(0);
        then(result.getTotalUsers().getTotal()).isEqualTo(0);
        
    }

    @Test(expected=RepositoryException.class)
    public void getUserAssignmentThrowsException() throws IOException, ConnectionException {
        CassandraAssignmentsRepository cassandraAssignmentsRepository = new CassandraAssignmentsRepository(
                cassandraRepository, dbRepository, assignmentsRepository, cassandraDriver, experimentsKeysapce, eventLog, 5, true, true, true, true, "yyyy-mm-dd");
        Date from_time = new Date();
        given(cassandraDriver.getKeyspace()).willReturn(keyspace);
        
        given(keyspace.prepareQuery(Matchers.<ColumnFamily<User.ID,String>>any())).willReturn(query);
        
		given(query.withCql(isA(String.class))).willReturn(cqlQueryUserIdString);
        given(cqlQueryUserIdString.asPreparedStatement()).willReturn(preparedCqlQueryUserIdStringUserIdString);
        given(preparedCqlQueryUserIdStringUserIdString.withByteBufferValue(isA(User.ID.class), isA(Serializer.class))).
        	willReturn(preparedCqlQueryUserIdStringUserIdString);
        given(preparedCqlQueryUserIdStringUserIdString.withByteBufferValue(Mockito.isA(Application.Name.class), 
        		isA(Serializer.class))).	
    		willReturn(preparedCqlQueryUserIdStringUserIdString);
        given(preparedCqlQueryUserIdStringUserIdString.withStringValue(isA(String.class))).willReturn(preparedCqlQueryUserIdStringUserIdString);
        ConnectionException ce = new HostDownException("test");
        given(preparedCqlQueryUserIdStringUserIdString.execute()).willThrow(ce);
        User.ID userID = User.ID.valueOf("u1");
        Application.Name appLabel = Application.Name.valueOf("a1");
        Context context = Context.valueOf("c1");
        
        CatchExceptionBdd.when(cassandraAssignmentsRepository.getUserAssignments(userID, appLabel, context));
     }
}
