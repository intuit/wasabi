package com.intuit.wasabi.repository.cassandra.impl;

import static org.junit.Assert.*;
import io.codearte.catchexception.shade.mockito.Mockito;

import java.util.Date;
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
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.MutexAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.Exclusion;

@RunWith(MockitoJUnitRunner.class)
public class CassandraMutexRepositoryTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) MutexAccessor accessor;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) ExperimentAccessor experimentAccessor;
    
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Result<com.intuit.wasabi.repository.cassandra.pojo.Exclusion> resultDatastax;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    Result<com.intuit.wasabi.repository.cassandra.pojo.Experiment> resultExperimentDatastax;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    CassandraDriver driver;
    
    CassandraMutexRepository repository;
    
    Application.Name applicationName;
    
    List<Experiment.ID> priorityIds;

	private ID base;

	private ID pair;

	private List<Exclusion> exclusions;
	private List<com.intuit.wasabi.repository.cassandra.pojo.Experiment> experiments;

	private Session session;
	private com.intuit.wasabi.repository.cassandra.pojo.Experiment experiment;
    
    @Before
    public void setUp() throws Exception {
    	accessor = Mockito.mock(MutexAccessor.class);
    	experimentAccessor = Mockito.mock(ExperimentAccessor.class);
    	resultDatastax = Mockito.mock(Result.class);
    	resultExperimentDatastax = Mockito.mock(Result.class);
    	driver = Mockito.mock(CassandraDriver.class);
    	session = Mockito.mock(Session.class);
    	Mockito.when(driver.getSession()).thenReturn(session);
    	repository = new CassandraMutexRepository(experimentAccessor, accessor, driver);
    	base = Experiment.ID.newInstance();
    	pair = Experiment.ID.newInstance();
    	exclusions = new ArrayList<>();
    	experiments = new ArrayList<>();
    	
		UUID experimentId = UUID.randomUUID();
		experiment = 
				new com.intuit.wasabi.repository.cassandra.pojo.Experiment();
		experiment.setId(experimentId);
		experiment.setAppName("ap1");
		experiment.setCreated(new Date());
		experiment.setStartTime(new Date());
		experiment.setEndTime(new Date());
		experiment.setModified(new Date());
		experiment.setLabel("l1");
		experiment.setSamplePercent(.5d);
		experiment.setState(Experiment.State.DRAFT.name());

    }
    
	@Test
	public void testGetNonExclusionsSuccess() {
		Exclusion exc = new Exclusion(base.getRawID(), pair.getRawID());
		exclusions.add(exc);
				
		experiments.add(experiment);
		
		Mockito.when(accessor.getExclusions(base.getRawID())).thenReturn(resultDatastax);
		Mockito.when(resultDatastax.all()).thenReturn(exclusions);
		
		Mockito.when(experimentAccessor.getExperimentById(Mockito.any())).thenReturn(resultExperimentDatastax);
		Mockito.when(resultExperimentDatastax.one())
				.thenReturn(experiment);
		Mockito.when(experimentAccessor.getExperimentByAppName(Mockito.anyString()))
			.thenReturn(resultExperimentDatastax);
		Mockito.when(resultExperimentDatastax.all()).thenReturn(experiments);
		
		ExperimentList result = repository.getNotExclusions(base);
		
		assertEquals("Value should be eq", 1, result.getExperiments().size());
	
	}

	@Test(expected=RepositoryException.class)
	public void testGetNonExclusionsThrowsException() {
		
		Mockito.when(accessor.getExclusions(base.getRawID())).thenThrow(new RuntimeException("runtimeexcp"));
		ExperimentList result = repository.getNotExclusions(base);		
	}

	@Test
	public void testGetExclusionsSuccess() {
		Exclusion exc = new Exclusion(base.getRawID(), pair.getRawID());
		exclusions.add(exc);
	
		experiments.add(experiment);
		
		Mockito.when(accessor.getExclusions(base.getRawID())).thenReturn(resultDatastax);
		Mockito.when(resultDatastax.all()).thenReturn(exclusions);
		
		Mockito.when(experimentAccessor.getExperiments(Mockito.any())).thenReturn(resultExperimentDatastax);
		Mockito.when(resultExperimentDatastax.all())
				.thenReturn(experiments);
		ExperimentList result = repository.getExclusions(base);
		
		assertEquals("Value should be eq", 1, result.getExperiments().size());
		assertEquals("value should be same", experiment.getId(), 
				result.getExperiments().get(0).getID().getRawID());
	
	}

	@Test(expected=RepositoryException.class)
	public void testGetExclusionsThrowsException() {
		
		Mockito.when(accessor.getExclusions(base.getRawID())).thenThrow(
				new RuntimeException("RuntimeException"));
		ExperimentList result = repository.getExclusions(base);		
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
