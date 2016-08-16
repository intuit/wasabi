package com.intuit.wasabi.repository.cassandra.impl;

import static org.junit.Assert.*;
import io.codearte.catchexception.shade.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.datastax.driver.mapping.Result;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;

@RunWith(MockitoJUnitRunner.class)
public class CassandraPrioritiesRepositoryTest {

    @Mock
    PrioritiesAccessor accessor;
    
    @Mock
    Result<com.intuit.wasabi.repository.cassandra.pojo.Application> resultDatastax;

    @Mock
	Result<com.intuit.wasabi.repository.cassandra.pojo.Application> result;
    
    CassandraPrioritiesRepository repository;
    
    Application.Name applicationName;
    
    List<Experiment.ID> priorityIds;
    
    @Before
    public void setUp() throws Exception {
    	accessor = Mockito.mock(PrioritiesAccessor.class);
    	resultDatastax = Mockito.mock(Result.class);
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
	public void testCreatePrioritiesSuccess() {
		priorityIds.add(Experiment.ID.newInstance());
		repository.createPriorities(applicationName, priorityIds);
		Mockito.verify(accessor).updatePriorities(Mockito.anyList(), Mockito.anyString());
	}

	@Test(expected=RepositoryException.class)
	public void testCreatePrioritiesDeletionThrowsException() {
		Mockito.doThrow(new RuntimeException("RuntimeException")).when(accessor).deletePriorities(Mockito.anyString());
		repository.createPriorities(applicationName, priorityIds);
		Mockito.verify(accessor,Mockito.atLeastOnce()).
			deletePriorities(applicationName.toString());
	}

	@Test(expected=RepositoryException.class)
	public void testCreatePrioritiesUpdateThrowsException() {
		priorityIds.add(Experiment.ID.newInstance());
		Mockito.doThrow(new RuntimeException("RuntimeException")).when(accessor).updatePriorities(Mockito.anyList(),
				Mockito.anyString());
		repository.createPriorities(applicationName, priorityIds);
		Mockito.verify(accessor,Mockito.atLeastOnce()).
			updatePriorities(Mockito.anyList(), Mockito.eq(applicationName.toString()));
	}

	@Test
	public void testGetPrioritiesListSuccess() {
		priorityIds.add(Experiment.ID.newInstance());
		ArrayList<UUID> ids = new ArrayList<>();
		ids.add(priorityIds.get(0).getRawID());
		com.intuit.wasabi.repository.cassandra.pojo.Application app = 
				new com.intuit.wasabi.repository.cassandra.pojo.Application(applicationName.toString(), ids);
		List<com.intuit.wasabi.repository.cassandra.pojo.Application> allApps = new ArrayList<>();
		allApps.add(app);
		Mockito.doReturn(allApps).when(resultDatastax).all();
		
		Mockito.doReturn(resultDatastax).when(accessor).getPriorities(applicationName.toString());
	    
		List<ID> priorities = repository.getPriorityList(applicationName);
	    assertEquals("Size should be same", 1, priorities.size());
	    assertEquals("Value should be equal", 
	    		priorityIds.get(0),priorities.get(0));
		Mockito.verify(accessor).getPriorities(applicationName.toString());
	}

	@Test(expected=RepositoryException.class)
	public void testGetPrirotiesListThrowsException() {
		Mockito.doThrow(new RuntimeException("RuntimeException")).when(accessor).
			getPriorities(Mockito.anyString());
		repository.getPriorityList(applicationName);
	}

	@Test
	public void testGetPrioritiesListLengthSuccess() {
		priorityIds.add(Experiment.ID.newInstance());
		ArrayList<UUID> ids = new ArrayList<>();
		ids.add(priorityIds.get(0).getRawID());
		com.intuit.wasabi.repository.cassandra.pojo.Application app = 
				new com.intuit.wasabi.repository.cassandra.pojo.Application(applicationName.toString(), ids);
		List<com.intuit.wasabi.repository.cassandra.pojo.Application> allApps = new ArrayList<>();
		allApps.add(app);
		Mockito.doReturn(allApps).when(resultDatastax).all();
		
		Mockito.doReturn(resultDatastax).when(accessor).getPriorities(applicationName.toString());
	    
		int size = repository.getPriorityListLength(applicationName);
	    assertEquals("Size should be same", 1, size);
		Mockito.verify(accessor).getPriorities(applicationName.toString());
	}

	@Test(expected=RepositoryException.class)
	public void testGetPrirotiesListLengthThrowsException() {
		Mockito.doThrow(new RuntimeException("RuntimeException")).when(accessor).
			getPriorities(Mockito.anyString());
		repository.getPriorityListLength(applicationName);
	}
}
