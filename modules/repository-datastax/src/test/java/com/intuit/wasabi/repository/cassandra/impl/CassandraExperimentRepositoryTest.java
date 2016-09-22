package com.intuit.wasabi.repository.cassandra.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.datastax.driver.mapping.Result;
import com.google.common.collect.Table;
import com.intuit.wasabi.analyticsobjects.counts.AssignmentCounts;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.experimentobjects.Application;
import com.intuit.wasabi.experimentobjects.Application.Name;
import com.intuit.wasabi.experimentobjects.Bucket;
import com.intuit.wasabi.experimentobjects.Bucket.State;
import com.intuit.wasabi.experimentobjects.BucketList;
import com.intuit.wasabi.experimentobjects.Context;
import com.intuit.wasabi.experimentobjects.Experiment;
import com.intuit.wasabi.experimentobjects.Experiment.ID;
import com.intuit.wasabi.experimentobjects.Experiment.Label;
import com.intuit.wasabi.experimentobjects.ExperimentList;
import com.intuit.wasabi.experimentobjects.NewExperiment;
import com.intuit.wasabi.repository.AuditLogRepository;
import com.intuit.wasabi.repository.RepositoryException;
import com.intuit.wasabi.repository.cassandra.IntegrationTestBase;
import com.intuit.wasabi.repository.cassandra.accessor.ApplicationListAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.BucketAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.audit.BucketAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.audit.ExperimentAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentLabelIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.StateExperimentIndexAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.UserBucketIndexAccessor;

public class CassandraExperimentRepositoryTest extends IntegrationTestBase  {

    ExperimentAccessor experimentAccessor;

    CassandraExperimentRepository repository;

	private ID experimentID1;
	private ID experimentID2;

	private Bucket bucket1;
	private Bucket bucket2;

	private NewExperiment newExperiment1;
	private NewExperiment newExperiment2;

	private ExperimentLabelIndexAccessor mockExperimentLabelIndexAccessor;
	private ApplicationListAccessor mockApplicationListAccessor;

	private BucketAccessor mockBucketAccessor;

	private Context QA = Context.valueOf("qa");
	private Application.Name appname;

	private BucketAuditLogAccessor bucketAuditLogAccessor;
	private ExperimentAuditLogAccessor experimentAuditLogAccessor;

	private CassandraDriver mockDriver;

	private ExperimentAccessor mockExperimentAccessor;

	private StateExperimentIndexAccessor mockStateExperimentIndexAccessor;

	private UserBucketIndexAccessor mockUserBucketIndexAccessor;

	private BucketAuditLogAccessor mockBucketAuditLogAccessor;

	private ExperimentAuditLogAccessor mockExperimentAuditLogAccessor;
	
    @Before
    public void setUp() throws Exception {
    	
    	mockBucketAccessor = Mockito.mock(BucketAccessor.class);
    	mockDriver = Mockito.mock(CassandraDriver.class);
    	mockExperimentAccessor = Mockito.mock(ExperimentAccessor.class);
    	mockStateExperimentIndexAccessor = Mockito.mock(StateExperimentIndexAccessor.class);
    	mockUserBucketIndexAccessor = Mockito.mock(UserBucketIndexAccessor.class);
    	mockBucketAuditLogAccessor = Mockito.mock(BucketAuditLogAccessor.class);
    	mockExperimentAuditLogAccessor = Mockito.mock(ExperimentAuditLogAccessor.class);
    	mockApplicationListAccessor = Mockito.mock(ApplicationListAccessor.class);
    	mockExperimentLabelIndexAccessor = Mockito.mock(ExperimentLabelIndexAccessor.class);
    	IntegrationTestBase.setup();

    	if (repository != null) return;

        appname = Application.Name.valueOf("app1" + System.currentTimeMillis());
        
        experimentAccessor = injector.getInstance(ExperimentAccessor.class);
        bucketAuditLogAccessor = injector.getInstance(BucketAuditLogAccessor.class);
        experimentAuditLogAccessor = injector.getInstance(ExperimentAuditLogAccessor.class);
        session.execute("truncate wasabi_experiments.bucket");
        
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
    	newExperiment1.setApplicationName(appname);
    	newExperiment1.setLabel(Experiment.Label.valueOf("el1" + System.currentTimeMillis()));
    	newExperiment1.setCreatorID("c1");
    	newExperiment1.setDescription("ed1");
    	newExperiment1.setStartTime(new Date());
    	newExperiment1.setEndTime(new Date());
    	newExperiment1.setSamplingPercent(0.2);
    	experimentID1 = repository.createExperiment(newExperiment1);
    	
    	repository.createIndicesForNewExperiment(newExperiment1);

    	newExperiment2 = new NewExperiment(experimentID2);
    	newExperiment2.setApplicationName(appname);
    	newExperiment2.setLabel(Experiment.Label.valueOf("el2" + System.currentTimeMillis()));
    	newExperiment2.setCreatorID("c2");
    	newExperiment2.setDescription("ed2");
    	newExperiment2.setStartTime(new Date());
    	newExperiment2.setEndTime(new Date());
    	newExperiment2.setSamplingPercent(0.2);
    	experimentID2 = repository.createExperiment(newExperiment2);
    	
    	repository.createIndicesForNewExperiment(newExperiment2);
    }
    
	@Test(expected=RepositoryException.class)
	public void testCreateBatchBucketSessionNullThrowsException() {
		BucketList bucketList = new BucketList();
		bucketList.addBucket(bucket1);
		bucketList.addBucket(bucket2);
		repository.setDriver(mockDriver);
		repository.updateBucketBatch(experimentID1, bucketList);
	}

	@Test(expected=RepositoryException.class)
	public void testCreateBucketAccessorMockThrowsException() {
		repository.setBucketAccessor(mockBucketAccessor);
		Mockito.doThrow(new RuntimeException("testexception")).when(
				mockBucketAccessor).insert(bucket1.getExperimentID().getRawID(), 
				bucket1.getLabel().toString(), bucket1.getDescription(), bucket1.getAllocationPercent(), 
				bucket1.isControl(), bucket1.getPayload(), Bucket.State.OPEN.name());
		repository.createBucket(bucket1);
	}

	@Test(expected=RepositoryException.class)
	public void testRemoveExperimentLabelIndexAccessorMockThrowsException() {
		repository.setExperimentLabelIndexAccessor(mockExperimentLabelIndexAccessor);
		Mockito.doThrow(new RuntimeException("testexception")).when(
				mockExperimentLabelIndexAccessor).deleteBy(Mockito.any(), Mockito.any());
		repository.removeExperimentLabelIndex(appname,newExperiment1.getLabel());
	}

	@Test(expected=RepositoryException.class)
	public void testGetDeleteExperimentAccessorMockThrowsException() {
		repository.setExperimentAccessor(mockExperimentAccessor);
		Mockito.doThrow(new RuntimeException("test")).when(mockExperimentAccessor).deleteExperiment(Mockito.any());
		repository.deleteExperiment(newExperiment1);
	}
	
	@Test(expected=RepositoryException.class)
	public void testGetExperimentByAppAccessorMockThrowsException() {
		repository.setExperimentAccessor(mockExperimentAccessor);
		Table<ID, Label, Experiment> experiments = 
				repository.getExperimentList(newExperiment1.getApplicationName());
	}

	@Test(expected=RepositoryException.class)
	public void testCreateAppGetApplicationAccessorMockThrowsException() {
		int size = repository.getApplicationsList().size();
		Name app = Application.Name.valueOf("appx" + System.currentTimeMillis());
		repository.createApplication(app);
		
		repository.setApplicationListAccessor(mockApplicationListAccessor);
		
		List<Name> apps = repository.getApplicationsList();
		assertEquals("Value should be equal", size + 1, apps.size());
		assertTrue("App should be in the list " + apps, apps.stream().anyMatch(application -> application.toString().equals(app.toString())));
	}

	@Test(expected=RepositoryException.class)
	public void testCreateAppApplicationListMockAccessorThrowsException() {

		repository.setApplicationListAccessor(mockApplicationListAccessor);
		Mockito.doThrow(new RuntimeException("runtime")).when(mockApplicationListAccessor).insert(Mockito.anyString());
		repository.createApplication(appname);
		
	}


	@Test(expected=RepositoryException.class)
	public void testGetExperimentAccessorMockThrowsException() {
		repository.setExperimentAccessor(mockExperimentAccessor);
		Experiment experiment = repository.getExperiment(experimentID1);		
	}

	@Test(expected=RepositoryException.class)
	public void testGetExperimentLabelIndexAccessorMockThrowsException() {
		repository.setExperimentLabelIndexAccessor(mockExperimentLabelIndexAccessor);
		Experiment experiment = repository.getExperiment(appname, newExperiment1.getLabel());		
	}

	@Test(expected=RepositoryException.class)
	public void testGetExperimentsAccessorMockThrowsException() {
		List<Experiment.ID> experimentIds = new ArrayList<>();
		experimentIds.add(experimentID1);
		experimentIds.add(experimentID2);
		repository.setExperimentAccessor(mockExperimentAccessor);
		 ExperimentList experiments = repository.getExperiments(experimentIds);
	}

	@Test(expected=RepositoryException.class)
	public void testGetExperimentsByAppNameAccessorMockThrowsException() {
		repository.setExperimentAccessor(mockExperimentAccessor);
		List<Experiment> experiments = repository.getExperiments(appname);
	}

	@Test(expected=RepositoryException.class)
	public void testGetExperimentsStateExperimentAccesorMockSuccess() {
		repository.setStateExperimentIndexAccessor(mockStateExperimentIndexAccessor);
		repository.getExperiments();
	}

	@Test(expected=RepositoryException.class)
	public void testGetOneBucketListWithBucketAccessorMockThrowsException() {
		 repository.setBucketAccessor(mockBucketAccessor);
		 Mockito.doThrow(new RuntimeException("testexception")).when(
					mockBucketAccessor).getBucketByExperimentId(null);

		 Object buckets = repository.getBucketList((Experiment.ID)null);
	}

	@Test(expected=RepositoryException.class)
	public void testGetBucketWithBucketAccessorMockThrowsException() {
		 repository.setBucketAccessor(mockBucketAccessor);
		 Bucket buckets = repository.getBucket(bucket1.getExperimentID(), bucket1.getLabel());
	}

	@Test(expected=RepositoryException.class)
	public void testGetOneBucketListBucketAccessorMock() {
		BucketList bucketList1 = new BucketList();
		bucketList1.addBucket(bucket1);
		
		repository.updateBucketBatch(experimentID1, bucketList1);

		List<Experiment.ID> experimentIds = new ArrayList<>();
		experimentIds.add(experimentID1);
		experimentIds.add(experimentID2);
		
		repository.setBucketAccessor(mockBucketAccessor);
		
		Map<ID, BucketList> buckets = repository.getBucketList(experimentIds);
	}

	@Test(expected=RepositoryException.class)
	public void testUpdateBucketAccessorMockThrowsException() {
		repository.setBucketAccessor(mockBucketAccessor);
		repository.updateBucket(bucket1);
		Bucket bucketResult = repository.updateBucket(bucket1);

	}

	@Test(expected=RepositoryException.class)
	public void testUpdateBucketNotControlAccessorMockThrowsException() {
		repository.setBucketAccessor(mockBucketAccessor);
		repository.updateBucket(bucket2);
		Bucket bucketResult = repository.updateBucket(bucket1);

	}

	@Test(expected=RepositoryException.class)
	public void testUpdateBucketAllocationAccessorMockThrowsException() {
		repository.setBucketAccessor(mockBucketAccessor);
		repository.updateBucketAllocationPercentage(bucket1,.01);
	}

	@Test(expected=RepositoryException.class)
	public void testUpdateExperimentAccessorMockThrowsException() {
		Experiment experiment = repository.getExperiment(experimentID1);
		repository.setExperimentAccessor(mockExperimentAccessor);
		repository.setExperimentLabelIndexAccessor(mockExperimentLabelIndexAccessor);
		Mockito.doThrow(new RuntimeException("test")).when(mockExperimentLabelIndexAccessor).updateBy(
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), 
				Mockito.any());
		 
		repository.updateExperiment(experiment);
	}

	@Test(expected=RepositoryException.class)
	public void testUpdateExperimentStateAccessorMockThrowsException() {
		Experiment experiment = repository.getExperiment(experimentID1);
		repository.setExperimentAccessor(mockExperimentAccessor);
		Mockito.doThrow(new RuntimeException("test")).when(mockExperimentAccessor).updateExperiment(Mockito.any(), Mockito.any(), Mockito.any());
		
		repository.updateExperimentState(experiment, Experiment.State.PAUSED);
	}

	@Test(expected=RepositoryException.class)
	public void testUpdateExperimentExperimentAccessorThrowsException() {

		Experiment experiment = repository.getExperiment(experimentID1);
		String description = "newDescription" + System.currentTimeMillis();
		experiment.setDescription(description);
		repository.setExperimentAccessor(null);
		repository.updateExperiment(experiment);
	}

	@Test(expected=RepositoryException.class)
	public void testUpdateExperimentExperimentLabelIndexAccessorMockThrowsException() {

		Experiment experiment = repository.getExperiment(experimentID1);
		String description = "newDescription" + System.currentTimeMillis();
		experiment.setDescription(description);
		repository.setExperimentLabelIndexAccessor(mockExperimentLabelIndexAccessor);
		Mockito.doThrow(new RuntimeException("test")).when(mockExperimentLabelIndexAccessor).
			updateBy(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), 
					Mockito.any(), Mockito.any());
		repository.updateExperiment(experiment);
		
	}

	@Test(expected=RepositoryException.class)
	public void testUpdateStateIndexAccessorMockThrowsException() {

		Experiment experiment = repository.getExperiment(experimentID1);
		assertEquals("Value should be eq", Experiment.State.DRAFT, experiment.getState());
		repository.setStateExperimentIndexAccessor(mockStateExperimentIndexAccessor);
		repository.updateStateIndex(experiment);
		
	}

	@Test(expected=RepositoryException.class)
	public void testUpdateBucketStateAccessorMockThrowsException() {
		repository.setBucketAccessor(mockBucketAccessor);
		Bucket resultBucket = repository.updateBucketState(bucket1, Bucket.State.CLOSED);
	}

	@Test(expected=NullPointerException.class)
	public void testGetExperimentByAppNullThrowsException() {
		Experiment experiment = repository.getExperiment(null,newExperiment1.getLabel());
	}
	
	@Test(expected=NullPointerException.class)
	public void testGetExperimentByLabelNullThrowsException() {
		Experiment experiment = repository.getExperiment(newExperiment1.getApplicationName(),null);
		
	}

	@Test(expected=RepositoryException.class)
	public void testGetAssigmentsCountWithAccessorMockThrowsException() {
		repository.setUserBucketIndexAccessor(mockUserBucketIndexAccessor);
		AssignmentCounts count = repository.getAssignmentCounts(experimentID1, QA);
	}

	@Test(expected=RepositoryException.class)
	public void testLogBucketAuditAccessorMockThrowsException() {
		String bucketLabel = "bkt" + System.currentTimeMillis();
		Result<?> logs = bucketAuditLogAccessor.selectBy(experimentID1.getRawID(), bucketLabel);
	
		List<Bucket.BucketAuditInfo> auditLog = new ArrayList<>();
		Bucket.BucketAuditInfo log = new Bucket.BucketAuditInfo("attr1", "o1", "n1");
		auditLog.add(log);
		repository.setBucketAuditLogAccessor(mockBucketAuditLogAccessor);
		Mockito.doThrow(new RuntimeException("runtime")).when(mockBucketAuditLogAccessor).insertBy(
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		repository.logBucketChanges(experimentID1, Bucket.Label.valueOf(bucketLabel), auditLog );
	}

	@Test(expected=RepositoryException.class)
	public void testLogExperimentAuditMockAccessorThrowsException() {
		Experiment.ID expid = Experiment.ID.newInstance();
		
		List<Experiment.ExperimentAuditInfo> auditLog = new ArrayList<>();
		Experiment.ExperimentAuditInfo log = new Experiment.ExperimentAuditInfo("attr1", "o1", "n1");
		auditLog.add(log);
		repository.setExperimentAuditLogAccessor(mockExperimentAuditLogAccessor);
		Mockito.doThrow(new RuntimeException("runtime")).when(mockExperimentAuditLogAccessor)
		.insertBy(
				Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
		repository.logExperimentChanges(expid, auditLog);

	}

	// NOTE - IMHO - The createIndicesForNewExperiment should not be in the interface  
	@Test(expected=RepositoryException.class)
	public void testCreateIndexesForExperimentStateIndexAccessorMockThrowsException() {
		newExperiment1.setId(Experiment.ID.newInstance());
		repository.setStateExperimentIndexAccessor(mockStateExperimentIndexAccessor);
		repository.createIndicesForNewExperiment(newExperiment1);		
	}

	@Test(expected=RepositoryException.class)
	public void testGetBucketsAccessorMockThrowsException() {
		repository.setBucketAccessor(mockBucketAccessor);
		BucketList buckets = repository.getBuckets(experimentID1);
	}

	@Test(expected=RepositoryException.class)
	public void testDeleteBucketAccessorMockThrowsException() {
		repository.setBucketAccessor(mockBucketAccessor);
		Mockito.doThrow(new RuntimeException("testexception")).when(
				mockBucketAccessor).deleteByExperimentIdAndLabel(bucket1.getExperimentID().getRawID(), 
				bucket1.getLabel().toString());
		repository.deleteBucket(experimentID1, bucket1.getLabel());		
	}

	@Test(expected=RepositoryException.class)
	public void testCreateExperimentAccessorMockThrowsException() {
		newExperiment1.setId(Experiment.ID.newInstance());
		newExperiment1.setLabel(Experiment.Label.valueOf("lbl" + System.currentTimeMillis()));
		repository.setExperimentAccessor(mockExperimentAccessor);
		repository.setApplicationListAccessor(mockApplicationListAccessor);
		Mockito.doThrow(new RuntimeException("runtime")).when(mockApplicationListAccessor).insert(Mockito.anyString());
		ID experimentId = repository.createExperiment(newExperiment1);		
	}

	@Test
	public void testGetterSetter() {
		assertNotNull("value should be not be null",  repository.getApplicationListAccessor());
		assertNotNull("value should be not be null",  repository.getBucketAuditLogAccessor());
		assertNotNull("value should be not be null",  repository.getExperimentAccessor());
		assertNotNull("value should be not be null",  repository.getExperimentAuditLogAccessor());
		assertNotNull("value should be not be null",  repository.getStateExperimentIndexAccessor());
		assertNotNull("value should be not be null",  repository.getExperimentAccessor());
		assertNotNull("value should be not be null",  repository.getBucketAccessor());
		assertNotNull("value should be not be null",  repository.getExperimentLabelIndexAccessor());
		assertNotNull("value should be not be null",  repository.getUserBucketIndexAccessor());
		assertNotNull("value should be not be null",  repository.getDriver());
		
		repository.setApplicationListAccessor(null);;
		assertEquals("Value should be eq", null, repository.getApplicationListAccessor());
		
		repository.setBucketAccessor(null);
		assertEquals("Value should be eq", null, repository.getBucketAccessor());

		repository.setExperimentAccessor(null);
		assertEquals("Value should be eq", null, repository.getExperimentAccessor());
		
		repository.setBucketAuditLogAccessor(null);
		assertEquals("Value should be eq", null, repository.getBucketAuditLogAccessor());

		repository.setExperimentAuditLogAccessor(null);
		assertEquals("Value should be eq", null, repository.getExperimentAuditLogAccessor());

		repository.setStateExperimentIndexAccessor(null);
		assertEquals("Value should be eq", null, repository.getStateExperimentIndexAccessor());

		repository.setUserBucketIndexAccessor(null);
		assertEquals("Value should be eq", null, repository.getUserBucketIndexAccessor());

		repository.setExperimentLabelIndexAccessor(null);
		assertEquals("Value should be eq", null, repository.getExperimentLabelIndexAccessor());

		repository.setDriver(null);
		assertEquals("Value should be eq", null, repository.getDriver());
	}
	
	@Test(expected=RepositoryException.class)
	public void testCreateExperimentExperimentAccessorMockThrowsException() {
		repository.setExperimentAccessor(mockExperimentAccessor);
		newExperiment1.setId(Experiment.ID.newInstance());;
		ID experimentId = repository.createExperiment(newExperiment1);		
	}

}
