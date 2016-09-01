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
import com.intuit.wasabi.experimentobjects.Application.Name;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CassandraExperimentRepositoryITest {

    ExperimentAccessor experimentAccessor;

    CassandraExperimentRepository repository;

	private Session session;

	private MappingManager manager;

	private String applicationName;

	private CassandraDriver driver;

	private ID experimentID1;
	private ID experimentID2;

	private Bucket bucket1;
	private Bucket bucket2;

	private NewExperiment newExperiment1;
	private NewExperiment newExperiment2;

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
        
		experimentID1 = Experiment.ID.valueOf(UUID.randomUUID());
		experimentID2 = Experiment.ID.valueOf(UUID.randomUUID());
		
    	repository = injector.getInstance(CassandraExperimentRepository.class);;
    	bucket1 = Bucket.newInstance(experimentID1,Bucket.Label.valueOf("bl1")).withAllocationPercent(.23)
    			.withControl(true)
    			.withDescription("b1").withPayload("p1")
    			.withState(State.OPEN).build();

    	bucket2 = Bucket.newInstance(experimentID2,Bucket.Label.valueOf("bl2"))
    			.withAllocationPercent(.24).withControl(false)
    			.withDescription("b2").withPayload("p2")
    			.withState(State.OPEN).build();
    	
    	newExperiment1 = new NewExperiment(experimentID1);
    	newExperiment1.setApplicationName(Application.Name.valueOf("app1" + System.currentTimeMillis()));
    	newExperiment1.setLabel(Experiment.Label.valueOf("el1" + System.currentTimeMillis()));
    	newExperiment1.setCreatorID("c1");
    	newExperiment1.setDescription("ed1");
    	newExperiment1.setStartTime(new Date());
    	newExperiment1.setEndTime(new Date());
    	newExperiment1.setSamplingPercent(0.2);
    	experimentID1 = repository.createExperiment(newExperiment1);
    	
    	repository.createIndicesForNewExperiment(newExperiment1);

    	newExperiment2 = new NewExperiment(experimentID2);
    	newExperiment2.setApplicationName(Application.Name.valueOf("app1" + System.currentTimeMillis()));
    	newExperiment2.setLabel(Experiment.Label.valueOf("el2" + System.currentTimeMillis()));
    	newExperiment2.setCreatorID("c2");
    	newExperiment2.setDescription("ed2");
    	newExperiment2.setStartTime(new Date());
    	newExperiment2.setEndTime(new Date());
    	newExperiment2.setSamplingPercent(0.2);
    	experimentID2 = repository.createExperiment(newExperiment2);
    	
    	repository.createIndicesForNewExperiment(newExperiment2);
}
    
	@Test
	public void testCreateBatchBucketWith2BucketsSuccess() {
		BucketList bucketList = new BucketList();
		bucketList.addBucket(bucket1);
		bucketList.addBucket(bucket2);
		
		repository.updateBucketBatch(experimentID1, bucketList);
		
		BucketList result = repository.getBucketList(experimentID1);
		assertEquals("Value should be equal", 2, result.getBuckets().size());
	}

	@Test
	public void testGetExperimentSuccess() {
		Experiment experiment = repository.getExperiment(experimentID1);
		
		assertEquals("Value should be equal", experimentID1, experiment.getID());
	}

	@Test
	public void testCreateAnnGetApplicationSuccess() {
		int size = repository.getApplicationsList().size();
		Name app = Application.Name.valueOf("appx" + System.currentTimeMillis());
		repository.createApplication(app);
		
		List<Name> apps = repository.getApplicationsList();
		assertEquals("Value should be equal", size + 1, apps.size());
		assertTrue("App should be in the list " + apps, 
				apps.stream().anyMatch(application -> application.toString().equals(app.toString())));
	}

	@Test(expected=NullPointerException.class)
	public void testGetExperimentNullThrowsException() {
		Experiment experiment = repository.getExperiment(null);		
	}

	@Test
	public void testGetExperimentByAppAndLabelSuccess() {
		Experiment experiment = repository.getExperiment(newExperiment1.getApplicationName(),
				newExperiment1.getLabel());
		assertEquals("Value should be equal", experimentID1, experiment.getID());
	}

	@Test
	public void testGetExperimentNoBucketsByIds() {
		List<Experiment.ID> experimentIds = new ArrayList<>();
		experimentIds.add(experimentID1);
		experimentIds.add(experimentID2);
		Map<ID, BucketList> buckets = repository.getBucketList(experimentIds);
		assertEquals("Value should be equal", 0, buckets.size());
	}

	@Test
	public void testGetOneBucketsEachByIds() {
		BucketList bucketList1 = new BucketList();
		bucketList1.addBucket(bucket1);
		
		repository.updateBucketBatch(experimentID1, bucketList1);

		BucketList bucketList2 = new BucketList();
		bucketList2.addBucket(bucket2);
		
		repository.updateBucketBatch(experimentID2, bucketList2);

		List<Experiment.ID> experimentIds = new ArrayList<>();
		experimentIds.add(experimentID1);
		experimentIds.add(experimentID2);
		Map<ID, BucketList> buckets = repository.getBucketList(experimentIds);
		assertEquals("Value should be equal", 2, buckets.size());
		assertEquals("Value should be equal", 1, buckets.get(experimentID1).getBuckets().size());
		assertEquals("Value should be equal", bucket1, buckets.get(experimentID1).getBuckets().get(0));
		assertEquals("Value should be equal", 1, buckets.get(experimentID2).getBuckets().size());
		assertEquals("Value should be equal", bucket2, buckets.get(experimentID2).getBuckets().get(0));

	}

	@Test
	public void testGetOneBucketsAndDeleteBucket() {
		BucketList bucketList1 = new BucketList();
		bucketList1.addBucket(bucket1);
		
		repository.updateBucketBatch(experimentID1, bucketList1);

		List<Experiment.ID> experimentIds = new ArrayList<>();
		experimentIds.add(experimentID1);
		Map<ID, BucketList> buckets = repository.getBucketList(experimentIds);
		assertEquals("Value should be equal", 1, buckets.size());
		assertEquals("Value should be equal", 1, buckets.get(experimentID1).getBuckets().size());
		assertEquals("Value should be equal", bucket1, buckets.get(experimentID1).getBuckets().get(0));

		repository.deleteBucket(experimentID1, bucket1.getLabel());
		
		buckets = repository.getBucketList(experimentIds);
		assertEquals("Value should be equal", 0, buckets.size());
		
	}

	@Test
	public void testUpdateBucketAllocation() {
		BucketList bucketList1 = new BucketList();
		bucketList1.addBucket(bucket1);
		
		repository.updateBucketBatch(experimentID1, bucketList1);

		List<Experiment.ID> experimentIds = new ArrayList<>();
		experimentIds.add(experimentID1);
		Map<ID, BucketList> buckets = repository.getBucketList(experimentIds);
		assertEquals("Value should be equal", 1, buckets.size());
		assertEquals("Value should be equal", 1, buckets.get(experimentID1).getBuckets().size());
		assertEquals("Value should be equal", bucket1, buckets.get(experimentID1).getBuckets().get(0));
		
		assertEquals("Value should be eq", bucket1.getAllocationPercent(),buckets.get(experimentID1).getBuckets().get(0).getAllocationPercent() );
		
		Bucket bucketResult = repository.updateBucketAllocationPercentage(bucket1, 0.75);
		assertEquals("Value should be eq", experimentID1, bucketResult.getExperimentID());
		assertEquals("Value should be eq", 0.75, bucketResult.getAllocationPercent(), 0.001);
		
	}

	@Test
	public void testUpdateBucketState() {
		BucketList bucketList1 = new BucketList();
		bucketList1.addBucket(bucket1);
		
		repository.updateBucketBatch(experimentID1, bucketList1);

		List<Experiment.ID> experimentIds = new ArrayList<>();
		experimentIds.add(experimentID1);
		Map<ID, BucketList> buckets = repository.getBucketList(experimentIds);
		assertEquals("Value should be equal", 1, buckets.size());
		assertEquals("Value should be equal", 1, buckets.get(experimentID1).getBuckets().size());
		assertEquals("Value should be equal", Bucket.State.OPEN, buckets.get(experimentID1).getBuckets().get(0).getState());

		Bucket resultBucket = repository.updateBucketState(bucket1, Bucket.State.CLOSED);
		assertEquals("Value should be eq", Bucket.State.CLOSED,resultBucket.getState());
		
	}

	@Test
	public void testGetTwoBucketsEachByIds() {
		BucketList bucketList1 = new BucketList();
		bucketList1.addBucket(bucket1);
		bucketList1.addBucket(bucket2);
		repository.updateBucketBatch(experimentID1, bucketList1);

		List<Experiment.ID> experimentIds = new ArrayList<>();
		experimentIds.add(experimentID1);
		Map<ID, BucketList> buckets = repository.getBucketList(experimentIds);
		assertEquals("Value should be equal", 1, buckets.size());
		assertEquals("Value should be equal", 2, buckets.get(experimentID1).getBuckets().size());
		BucketList bkts = buckets.get(experimentID1);
		List<Bucket> bucketsResponse = bkts.getBuckets();
		
		Collections.sort(bucketsResponse, new Comparator<Bucket>() {
			@Override
			public int compare(Bucket o1, Bucket o2) {
				return o1.getLabel().toString().compareTo(o2.getLabel().toString());
			}
		});
		
		List<Bucket> bucketsExpected = bucketList1.getBuckets();
		
		Collections.sort(bucketsExpected, new Comparator<Bucket>() {
			@Override
			public int compare(Bucket o1, Bucket o2) {
				return o1.getLabel().toString().compareTo(o2.getLabel().toString());
			}
		});
		
		assertTrue("lists should be eq", bucketsExpected.get(0).getLabel()
				.equals(bucketsResponse.get(0).getLabel()));
		assertTrue("lists should be eq", bucketsExpected.get(1).getLabel()
				.equals(bucketsResponse.get(1).getLabel()));
	}

	@Test(expected=NullPointerException.class)
	public void testGetExperimentByAppNullThrowsException() {
		Experiment experiment = repository.getExperiment(null,newExperiment1.getLabel());
	}
	
	@Test(expected=NullPointerException.class)
	public void testGetExperimentByLabelNullThrowsException() {
		Experiment experiment = repository.getExperiment(newExperiment1.getApplicationName(),null);
		
	}

	@Test
	public void testGetAssigmentsCountWithNoAssignmentSuccess() {
		BucketList bucketList = new BucketList();
		bucketList.addBucket(bucket1);
		repository.updateBucketBatch(experimentID1, bucketList);

		AssignmentCounts count = repository.getAssignmentCounts(experimentID1, QA);
		assertEquals("Value should be eq", 2, count.getAssignments().size());
		assertEquals("Value should be eq", bucket1.getLabel(), 
				count.getAssignments().get(0).getBucket());
		assertEquals("Value should be eq", newExperiment1.getId(), 
				count.getExperimentID());
		assertEquals("Value should be eq", 0, 
				count.getAssignments().get(0).getCount());
		
		assertEquals("Value should be eq", null, 
				count.getAssignments().get(1).getBucket());
		assertEquals("Value should be eq", 0, 
				count.getAssignments().get(1).getCount());
	}
}
