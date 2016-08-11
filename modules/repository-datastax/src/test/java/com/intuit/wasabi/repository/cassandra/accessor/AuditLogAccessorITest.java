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
package com.intuit.wasabi.repository.cassandra.accessor;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.AuditLogRepository;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.pojo.UserFeedback;
import com.intuit.wasabi.repository.cassandra.pojo.audit.AuditLog;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AuditLogAccessorITest {
    static Session session;
    static MappingManager manager;
    static AuditLogAccessor accessor;
    static Mapper<UserFeedback> mapper;
	private static String userId = "userid1";
	private static String applicationName;

    @BeforeClass
    public static void setup(){
        Injector injector = Guice.createInjector(new CassandraRepositoryModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));

        session = injector.getInstance(CassandraDriver.class).getSession();
        manager = new MappingManager(session);
        mapper = manager.mapper(UserFeedback.class);
        accessor = manager.createAccessor(AuditLogAccessor.class);
        applicationName = "ApplicationName:" + System.currentTimeMillis();
        session.execute("delete from wasabi_experiments.auditlog where application_name = '" 
        		+ applicationName + "'");
      

        Date time = new Date();
    	String action = AuditLogAction.BUCKET_CHANGED.toString();
    	String firstName = "fname";
    	String lastName = "lname";
    	String email = "e@mail";
    	String userName = "userName1";
    	UUID experimentId = UUID.randomUUID();
    	String experimentLabel = "ExpLabel1";
    	String bucketLabel = "bucketLabel1";
    	String changedProperty = "state";
    	String propertyBefore = "stateBefore";
    	String propertyAfter = "stateAfter";

    	for (int i = 0; i < 5; i++ )
    		accessor.storeEntry(applicationName + "-" + i, 
    			time, action, firstName, lastName, email, userName, userId, 
    			experimentId, experimentLabel, bucketLabel, changedProperty, propertyBefore, 
    			propertyAfter);
    	
    	for (int i = 0; i < 10; i++ )
    		accessor.storeEntry(AuditLogRepository.GLOBAL_ENTRY_APPLICATION.toString()+ "-" + i, 
    			time, action, firstName, lastName, email, userName, userId, 
    			experimentId, experimentLabel, bucketLabel, changedProperty, propertyBefore, 
    			propertyAfter);
    	
    }

    @Test
    public void testCreateAndGetAuditLogForEvent(){
    	Date time = new Date();
    	String action = AuditLogAction.BUCKET_CHANGED.toString();
    	String firstName = "fname";
    	String lastName = "lname";
    	String email = "e@mail";
    	String userName = "userName1";
    	UUID experimentId = UUID.randomUUID();
    	String experimentLabel = "ExpLabel1";
    	String bucketLabel = "bucketLabel1";
    	String changedProperty = "state";
    	String propertyBefore = "stateBefore";
    	String propertyAfter = "stateAfter";
    	
    	Result<AuditLog> resultBefore = accessor.getAuditLogEntryList(applicationName);    
    	List<AuditLog> auditLogs = resultBefore.all();
    	assertEquals("Audit log count should be same", 0, auditLogs.size());

    	accessor.storeEntry(applicationName, 
    			time, action, firstName, lastName, email, userName, userId, 
    			experimentId, experimentLabel, bucketLabel, changedProperty, propertyBefore, 
    			propertyAfter);
    	
    	Result<AuditLog> result = accessor.getAuditLogEntryList(applicationName);    
    	auditLogs = result.all();
    	assertEquals("Audit log count should be same", 1, auditLogs.size());
    	AuditLog log = auditLogs.get(0);
    	assertEquals("values should be equal",applicationName,log.getAppName());
    	assertEquals("values should be equal",time,log.getTime());
    	assertEquals("values should be equal",action,log.getAction());
    	assertEquals("values should be equal",firstName,log.getFirstName());
    	assertEquals("values should be equal",lastName,log.getLastName());
    	assertEquals("values should be equal",email,log.getEmail());
    	assertEquals("values should be equal",userName,log.getUsername());
    	assertEquals("values should be equal",userId,log.getUserId());
    	assertEquals("values should be equal",experimentId,log.getExperimentId());
    	assertEquals("values should be equal",experimentLabel,log.getExperimentLabel());
    	assertEquals("values should be equal",bucketLabel,log.getBucketLabel());
    	assertEquals("values should be equal",changedProperty,log.getProperty());
    	assertEquals("values should be equal",propertyBefore,log.getBefore());
    	assertEquals("values should be equal",propertyAfter,log.getAfter());
    	
    	result = accessor.getAuditLogEntryList(applicationName,5);    
    	auditLogs = result.all();
    	assertEquals("Audit log count should be same", 1, auditLogs.size());
    	AuditLog log2 = auditLogs.get(0);
    	assertEquals("Logs should be same", log, log2);
    	
    	result = accessor.getAuditLogEntryList(applicationName,1);    
    	auditLogs = result.all();
    	assertEquals("Audit log count should be same", 1, auditLogs.size());
    	log2 = auditLogs.get(0);
    	assertEquals("Logs should be same", log, log2);

    }

    @Test
    public void testGetCompleteAuditLogWithNoLimit(){
    	
    	Result<AuditLog> resultBefore = accessor.getCompleteAuditLogEntryList();    
    	List<AuditLog> auditLogs = resultBefore.all();
    	assertTrue("Audit log count should greater than zero", auditLogs.size() >= 15);

    }

    @Test
    public void testGetCompleteAuditLogWith5Limit(){
    	
    	Result<AuditLog> resultBefore = accessor.getCompleteAuditLogEntryList(5);    
    	List<AuditLog> auditLogs = resultBefore.all();
    	assertEquals("Audit log count should be equal", 5, auditLogs.size());

    }
}