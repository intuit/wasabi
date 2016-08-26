package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
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
import com.intuit.wasabi.repository.AuditLogRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.accessor.audit.AuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.audit.AuditLog;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CassandraAuditLogRepositoryITest {

    AuditLogAccessor auditLogAccessor;

	List<AuditLogEntry> result;
    
    CassandraAuditLogRepository repository;

	private AuditLogEntry entry;
	private AuditLogEntry entryWithMissingApplicationName;

	private Session session;

	private MappingManager manager;

	private Mapper<AuditLog> mapper;

	private String applicationName;

	private UserInfo userInfo;
    
    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new CassandraRepositoryModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));

        session = injector.getInstance(CassandraDriver.class).getSession();
        manager = new MappingManager(session);
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
				userInfo,AuditLogAction.BUCKET_CHANGED,
				Application.Name.valueOf(applicationName),
				Experiment.Label.valueOf("l1"),
				Experiment.ID.newInstance(), 
				Bucket.Label.valueOf("b1"), 
				"prop1", "v1", "v2");
		
		entryWithMissingApplicationName = 
				new AuditLogEntry(Calendar.getInstance(),
				userInfo,AuditLogAction.BUCKET_CHANGED,
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
	public void testSaveAndGetEntryWithEmptyUserExceptUsernameInfoSuccess() {
		// The reason for setting other attribs to "" is because the repository
		// code uses empty string. So to make the comparison by string , it is simpler
		// to make them empty strings
		UserInfo userInfo = UserInfo.newInstance(Username.valueOf("u1"))
				.withEmail("").withFirstName("").withLastName("").withUserId("").build();
		entry = 
				new AuditLogEntry(Calendar.getInstance(),
				userInfo,AuditLogAction.BUCKET_CHANGED,
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
				userInfo,AuditLogAction.BUCKET_CHANGED,
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


	@Test(expected=RepositoryException.class)
	public void testNullEntryThrowsRepositoryException() {
		repository.storeEntry(null);
	}
	
}
