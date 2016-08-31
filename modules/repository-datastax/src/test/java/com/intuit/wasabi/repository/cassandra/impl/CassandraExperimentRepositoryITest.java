package com.intuit.wasabi.repository.cassandra.impl;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.ExperimentValidator;
import com.intuit.wasabi.experimentobjects.Bucket.State;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.NewExperiment;
import com.intuit.wasabi.repository.AuditLogRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.accessor.ApplicationListAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.BucketAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentLabelIndexAccessor;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CassandraExperimentRepositoryITest {

    ExperimentAccessor experimentAccessor;

    CassandraExperimentRepository repository;

	private Session session;

	private MappingManager manager;

	private String applicationName;

	private CassandraDriver driver;

	private ID experimentID;

	private Bucket bucket1;
	private Bucket bucket2;

	private NewExperiment newExperiment;

	private ExperimentValidator validator;

	private ExperimentLabelIndexAccessor experimentLabelIndexAccessor;
	private ApplicationListAccessor applicationListAccessor;

	private BucketAccessor bucketAccessor;

	private Context QA = Context.valueOf("qa");
	
    @Before
    public void setUp() throws Exception {
        Injector injector = Guice.createInjector(new CassandraRepositoryModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));
       
        session = injector.getInstance(CassandraDriver.class).getSession();
        driver = injector.getInstance(CassandraDriver.class);
        manager = new MappingManager(session);
        
        experimentAccessor = injector.getInstance(ExperimentAccessor.class);
        bucketAccessor = injector.getInstance(BucketAccessor.class);
        experimentLabelIndexAccessor = injector.getInstance(
        		ExperimentLabelIndexAccessor.class);
        applicationListAccessor = injector.getInstance(ApplicationListAccessor.class);
        
        applicationName = "ApplicationName_" + System.currentTimeMillis();
        session.execute("truncate wasabi_experiments.bucket");
        
        validator = new ExperimentValidator(); 
        
        session.execute("delete from wasabi_experiments.auditlog where application_name = '" 
        		+ AuditLogRepository.GLOBAL_ENTRY_APPLICATION.toString() + "'");
		experimentID = Experiment.ID.newInstance();
		
    	repository = injector.getInstance(CassandraExperimentRepository.class);;
    	bucket1 = Bucket.newInstance(experimentID,Bucket.Label.valueOf("bl1")).withAllocationPercent(.23)
    			.withControl(true)
    			.withDescription("b1").withPayload("p1")
    			.withState(State.OPEN).build();

    	bucket2 = Bucket.newInstance(experimentID,Bucket.Label.valueOf("bl2"))
    			.withAllocationPercent(.24).withControl(false)
    			.withDescription("b2").withPayload("p2")
    			.withState(State.OPEN).build();
    	
    	newExperiment = new NewExperiment(experimentID);
    	newExperiment.setApplicationName(Application.Name.valueOf("app1" + System.currentTimeMillis()));
    	newExperiment.setLabel(Experiment.Label.valueOf("el1" + System.currentTimeMillis()));
    	newExperiment.setCreatorID("c1");
    	newExperiment.setDescription("ed1");
    	newExperiment.setStartTime(new Date());
    	newExperiment.setEndTime(new Date());
    	newExperiment.setSamplingPercent(0.2);
    	experimentID = repository.createExperiment(newExperiment);
    	
    	repository.createIndicesForNewExperiment(newExperiment);
    }
    
	@Test
	public void testCreateBatchBucketWith2BucketsSuccess() {
		BucketList bucketList = new BucketList();
		bucketList.addBucket(bucket1);
		bucketList.addBucket(bucket2);
		
		repository.updateBucketBatch(experimentID, bucketList);
		
		BucketList result = repository.getBucketList(experimentID);
		assertEquals("Value should be equal", 2, result.getBuckets().size());
	}

	@Test
	public void testGetExperimentSuccess() {
		Experiment experiment = repository.getExperiment(experimentID);
		
		assertEquals("Value should be equal", experimentID, experiment.getID());
	}

	@Test(expected=NullPointerException.class)
	public void testGetExperimentNullThrowsException() {
		Experiment experiment = repository.getExperiment(null);		
	}

	@Test
	public void testGetExperimentByAppAndLabelSuccess() {
		Experiment experiment = repository.getExperiment(newExperiment.getApplicationName(),newExperiment.getLabel());
		assertEquals("Value should be equal", experimentID, experiment.getID());
	}

	@Test(expected=NullPointerException.class)
	public void testGetExperimentByAppNullThrowsException() {
		Experiment experiment = repository.getExperiment(null,newExperiment.getLabel());
	}
	
	@Test(expected=NullPointerException.class)
	public void testGetExperimentByLabelNullThrowsException() {
		Experiment experiment = repository.getExperiment(newExperiment.getApplicationName(),null);
		
	}

	@Test
	public void testGetAssigmentsCountWithNoAssignmentSuccess() {
		BucketList bucketList = new BucketList();
		bucketList.addBucket(bucket1);
		repository.updateBucketBatch(experimentID, bucketList);

		AssignmentCounts count = repository.getAssignmentCounts(experimentID, QA);
		assertEquals("Value should be eq", 2, count.getAssignments().size());
		assertEquals("Value should be eq", bucket1.getLabel(), 
				count.getAssignments().get(0).getBucket());
		assertEquals("Value should be eq", newExperiment.getId(), 
				count.getExperimentID());
		assertEquals("Value should be eq", 0, 
				count.getAssignments().get(0).getCount());
		
		assertEquals("Value should be eq", null, 
				count.getAssignments().get(1).getBucket());
		assertEquals("Value should be eq", 0, 
				count.getAssignments().get(1).getCount());
	}
}
