/*******************************************************************************
 * Copyright 2016 Intuit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.intuit.wasabi.repository.cassandra.accessor.audit;

import static org.junit.Assert.assertEquals;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.accessor.audit.ExperimentAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.audit.ExperimentAuditLog;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

/**
 * These tests are just make sure that the queries work
 */
public class ExperimentAuditLogAccessorITest {
    static Session session;
    static MappingManager manager;
    static ExperimentAuditLogAccessor accessor;
    static UUID experimentId;
    static Date date;

    @BeforeClass
    public static void setupClass(){
        Injector injector = Guice.createInjector(new CassandraRepositoryModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));

        session = injector.getInstance(CassandraDriver.class).getSession();
        manager = new MappingManager(session);
        accessor = manager.createAccessor(ExperimentAuditLogAccessor.class);
        experimentId = UUID.randomUUID();
        date = new Date();
    }

    @Before
    public void setup() {
    	session.execute("truncate wasabi_experiments.experiment_audit_log");
    }
    
    @Test
    public void testCreateAndDeleteBucketAuditLog(){
    	Result<ExperimentAuditLog> result = accessor.selectBy(experimentId);
    	assertEquals("Value should be eq", 0, result.all().size());
    	
    	accessor.insertBy(experimentId, date, "a1", "v1", "v2");
    	
    	result = accessor.selectBy(experimentId);
    	assertEquals("Value should be eq", 1, result.all().size());

    	accessor.deleteBy(experimentId);
    	
    	result = accessor.selectBy(experimentId);
    	assertEquals("Value should be eq", 0, result.all().size());
    }

}