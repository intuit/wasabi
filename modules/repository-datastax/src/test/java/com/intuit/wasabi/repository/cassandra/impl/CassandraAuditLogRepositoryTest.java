package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.auditlogobjects.AuditLogAction;
import com.intuit.wasabi.auditlogobjects.AuditLogEntry;
import com.intuit.wasabi.authenticationobjects.UserInfo;
import com.intuit.wasabi.authenticationobjects.UserInfo.Username;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.feedbackobjects.UserFeedback;
import com.intuit.wasabi.repository.AuditLogRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.accessor.AuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.UserFeedbackAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.audit.AuditLog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

import org.mockito.runners.MockitoJUnitRunner;

import io.codearte.catchexception.shade.mockito.Mockito;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)    
public class CassandraAuditLogRepositoryTest {

    @Mock
    AuditLogAccessor accessor;

    @Mock
	Result<com.intuit.wasabi.repository.cassandra.pojo.audit.AuditLog> mockResult;
    
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
    			userInfo,AuditLogAction.BUCKET_CHANGED,
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
	public void testSaveAndGetEntrySuccess() {
		
		boolean success = repository.storeEntry(entry);
		
		assertEquals("value should be same", true, success);
		
		when(accessor.getAuditLogEntryList(entry.getApplicationName().toString()))
				.thenReturn(mockResult);
		when(mockResult.all()).thenReturn(dbEntries);
		
		List<AuditLogEntry> result = repository.getAuditLogEntryList(entry.getApplicationName());
		
		assertEquals("Value should be same", 1, result.size());
		assertEquals("Values should be same", dbEntry.getTime(),result.get(0).getTime().getTime());
		assertEquals("Values should be same", dbEntry.getAction(),result.get(0).getAction().name());

	}

	@Test(expected=RepositoryException.class)
	public void testGetEntryThrowsException() {
		
		when(accessor.getAuditLogEntryList(entry.getApplicationName().toString()))
				.thenThrow(new RuntimeException("testException"));
		
		List<AuditLogEntry> result = repository.getAuditLogEntryList(entry.getApplicationName());
	}

	@Test
	public void testGetCompleteEntrySuccess() {
		
		when(accessor.getCompleteAuditLogEntryList())
				.thenReturn(mockResult);
		when(mockResult.all()).thenReturn(dbEntries);
		
		List<AuditLogEntry> result = repository.getCompleteAuditLogEntryList();
		
		assertEquals("Value should be same", 1, result.size());
		assertEquals("Values should be same", dbEntry.getTime(),result.get(0).getTime().getTime());
		assertEquals("Values should be same", dbEntry.getAction(),result.get(0).getAction().name());

	}

	@Test
	public void testCompleteEntryWithLimitSuccess() {
		
		when(accessor.getCompleteAuditLogEntryList(1))
				.thenReturn(mockResult);
		when(mockResult.all()).thenReturn(dbEntries);
		
		List<AuditLogEntry> result = repository.getCompleteAuditLogEntryList(1);
		
		assertEquals("Value should be same", 1, result.size());
		assertEquals("Values should be same", dbEntry.getTime(),result.get(0).getTime().getTime());
		assertEquals("Values should be same", dbEntry.getAction(),result.get(0).getAction().name());

	}

	@Test(expected=RepositoryException.class)
	public void testGetCompleteEntryThrowsException() {
		
		when(accessor.getCompleteAuditLogEntryList())
				.thenThrow(new RuntimeException("testException"));
		
		List<AuditLogEntry> result = repository.getCompleteAuditLogEntryList();
	}
}
