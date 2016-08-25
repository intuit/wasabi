package com.intuit.wasabi.repository.cassandra.accessor;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.pojo.Experiment;
import com.intuit.wasabi.experimentobjects.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class ExperimentAccessorITest {
    static Session session;
    static MappingManager manager;
    static ExperimentAccessor accessor;
    static Mapper<Experiment> mapper;
    static UUID experimentId1 = UUID.randomUUID();
    static UUID experimentId2 = UUID.randomUUID();
    static Date date1 = new Date();
    static Date date2 = new Date();
    
    @BeforeClass
    public static void setup(){
        Injector injector = Guice.createInjector(new CassandraRepositoryModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));

        session = injector.getInstance(CassandraDriver.class).getSession();
        manager = new MappingManager(session);
        mapper = manager.mapper(Experiment.class);
        accessor = manager.createAccessor(ExperimentAccessor.class);
    }

    @Test
    public void insertOneExperiments() {
    	accessor.insertExperiment(experimentId1, 
    			"d1", "", 1.0, date1, date2, 
    			com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), "l1", 
    			"app1", date1, date2, true, 
    			"m1", "v1", true, 5000, "c1");
    	
    	Result<Experiment> experiment1 = accessor.getExperimentById(experimentId1);
    	List<Experiment> experimentResult = experiment1.all();
    	assertEquals("size should be same", 1, experimentResult.size());
    	Experiment exp = experimentResult.get(0);
    	assertEquals("Value should be same", experimentId1, exp.getId());
    	assertEquals("Value should be same", "d1", exp.getDescription());
    	assertEquals("Value should be same", "", exp.getRule());
    	assertEquals("Value should be same", 1.0, exp.getSamplePercent(), 0.0001d);
    	assertEquals("Value should be same", date1, exp.getStartTime());
    	assertEquals("Value should be same", date2, exp.getEndTime());
    	assertEquals("Value should be same", com.intuit.wasabi.experimentobjects.Experiment.State.DRAFT.name(), 
    			exp.getState());
    	assertEquals("Value should be same", "l1", exp.getLabel());
    	assertEquals("Value should be same", "app1", exp.getAppName());
    	assertEquals("Value should be same", date1, exp.getCreated());
    	assertEquals("Value should be same", date2, exp.getModified());
    	assertEquals("Value should be same", 5000, exp.getUserCap());
    	assertEquals("Value should be same", "c1", exp.getCreatorId());
    	
    }

}