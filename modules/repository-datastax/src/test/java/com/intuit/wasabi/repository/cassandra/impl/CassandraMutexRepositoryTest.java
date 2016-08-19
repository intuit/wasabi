package com.intuit.wasabi.repository.cassandra.impl;

import static org.junit.Assert.*;
import io.codearte.catchexception.shade.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.ReturnValues;
import org.mockito.runners.MockitoJUnitRunner;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Result;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.MutexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.Exclusion;

@RunWith(MockitoJUnitRunner.class)
public class CassandraMutexRepositoryTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) MutexAccessor accessor;
    
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Result<com.intuit.wasabi.repository.cassandra.pojo.Exclusion> resultDatastax;

    CassandraMutexRepository repository;
    
    Application.Name applicationName;
    
    List<Experiment.ID> priorityIds;

	private ID base;

	private ID pair;

	private List<Exclusion> exclusions;
    
    @Before
    public void setUp() throws Exception {
    	accessor = Mockito.mock(MutexAccessor.class);
    	resultDatastax = Mockito.mock(Result.class);
    	repository = new CassandraMutexRepository(null, accessor, null);
    	base = Experiment.ID.newInstance();
    	pair = Experiment.ID.newInstance();
    	exclusions = new ArrayList<>();
    }
    
    // TODO - This operation is not implemented yet !!!
	@Test(expected=UnsupportedOperationException.class)
	public void testGetPrioritiesSuccess() {
		
		System.err.println("!!!! - getProrities throws UnsupportedOperationException because it is not implemented!!!!");
		
		repository.getExclusions(null);
	}

	@Test
	public void testCreateExclusionSuccess() {
		repository.createExclusion(Experiment.ID.newInstance(), Experiment.ID.newInstance());
	}

	@Test(expected=RepositoryException.class)
	public void testCreateExclusionAccesorThrowsException() {
		Mockito.doThrow(new RuntimeException("RuntimeExcp")).when(accessor)
			.createExclusion(base.getRawID(), pair.getRawID());
		repository.createExclusion(base, pair);
	}

	@Test
	public void testDeleteExclusionSuccess() {
		repository.deleteExclusion(base, pair);
	}

	@Test(expected=RepositoryException.class)
	public void testDeleteExclusionAccesorThrowsException() {
		Mockito.doThrow(new RuntimeException("RuntimeExcp")).when(accessor)
			.deleteExclusion(base.getRawID(), pair.getRawID());
		repository.deleteExclusion(base, pair);
	}

	@Test
	public void testGetOneExclusionListSuccess() {
		Exclusion exc = new Exclusion(base.getRawID(), pair.getRawID());
		exclusions.add(exc);
		Mockito.when(accessor.getExclusions(base.getRawID())).thenReturn(resultDatastax);
		Mockito.when(resultDatastax.all()).thenReturn(exclusions);
		List<ID> result = repository.getExclusionList(base);
		
		assertEquals("value should be equal", pair, result.get(0));
	}

	@Test
	public void testGeZeroExclusionListSuccess() {
		Mockito.when(accessor.getExclusions(base.getRawID())).thenReturn(resultDatastax);
		Mockito.when(resultDatastax.all()).thenReturn(exclusions);
		List<ID> result = repository.getExclusionList(base);
		
		assertEquals("value should be equal", 0, result.size());
	}

	@Test(expected=RepositoryException.class)
	public void testGetOneExclusionListThrowsException() {
		Mockito.when(accessor.getExclusions(base.getRawID())).thenThrow(new RuntimeException("RTE"));
		List<ID> result = repository.getExclusionList(base);
		
	}
}
