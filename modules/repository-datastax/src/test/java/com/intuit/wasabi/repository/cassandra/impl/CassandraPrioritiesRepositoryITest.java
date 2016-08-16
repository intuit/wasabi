package com.intuit.wasabi.repository.cassandra.impl;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;

public class CassandraPrioritiesRepositoryITest {

    PrioritiesAccessor accessor;
    
    CassandraPrioritiesRepository repository;
    
    Application.Name applicationName;
    
    List<Experiment.ID> priorityIds;

	private Session session;

	private MappingManager manager;

	private Mapper<com.intuit.wasabi.repository.cassandra.pojo.Application> mapper;
    
    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new CassandraRepositoryModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));

        session = injector.getInstance(CassandraDriver.class).getSession();
        manager = new MappingManager(session);
        mapper = manager.mapper(com.intuit.wasabi.repository.cassandra.pojo.Application.class);
    	accessor = manager.createAccessor(PrioritiesAccessor.class);
    	repository = new CassandraPrioritiesRepository(accessor);
    	applicationName = Application.Name.valueOf("TestApplicationName");
    	priorityIds = new ArrayList<>();
    }
    
    // TODO - This operation is not implemented yet !!!
	@Test(expected=UnsupportedOperationException.class)
	public void testGetPrioritiesSuccess() {
		
		System.err.println("!!!! - getProrities throws UnsupportedOperationException because it is not implemented!!!!");
		
		repository.getPriorities(applicationName);
	}

	@Test
	public void testCreatePrioritiesOneIdSuccess() {
		priorityIds.add(Experiment.ID.newInstance());
		repository.createPriorities(applicationName, priorityIds);
		List<ID> priorityIdsList = repository.getPriorityList(applicationName);
		
		assertEquals("Size should be same", 1, priorityIdsList.size());
		assertEquals("Values should be same", priorityIdsList.get(0), priorityIds.get(0));
		
		int length = repository.getPriorityListLength(applicationName);
		assertEquals("Size should be same", 1, length);
	}

	@Test
	public void testCreatePrioritiesTwoIdsSuccess() {
		priorityIds.add(Experiment.ID.newInstance());
		priorityIds.add(Experiment.ID.newInstance());
		
		repository.createPriorities(applicationName, priorityIds);
		List<ID> priorityIdsList = repository.getPriorityList(applicationName);
		
		assertEquals("Size should be same", 2, priorityIdsList.size());
		assertEquals("Values should be same", priorityIdsList.get(0), 
				priorityIds.get(0));
		assertEquals("Values should be same", priorityIdsList.get(1), 
				priorityIds.get(1));
		int length = repository.getPriorityListLength(applicationName);
		assertEquals("Size should be same", 2, length);
	}

	@Test
	public void testCreatePrioritiesTwoIdsAndThenEmptyIdsSuccess() {
		priorityIds.add(Experiment.ID.newInstance());
		priorityIds.add(Experiment.ID.newInstance());
		
		repository.createPriorities(applicationName, priorityIds);
		List<ID> priorityIdsList = repository.getPriorityList(applicationName);
		
		assertEquals("Size should be same", 2, priorityIdsList.size());
		assertEquals("Values should be same", priorityIdsList.get(0),
				priorityIds.get(0));
		assertEquals("Values should be same", priorityIdsList.get(1), 
				priorityIds.get(1));
		int length = repository.getPriorityListLength(applicationName);
		assertEquals("Size should be same", 2, length);
		
		// Now empty list and create
		priorityIds.clear();
		repository.createPriorities(applicationName, priorityIds);
		priorityIdsList = repository.getPriorityList(applicationName);
		
		assertEquals("Size should be same", 0, priorityIdsList.size());
		length = repository.getPriorityListLength(applicationName);
		assertEquals("Size should be same", 0, length);		
	}
}
