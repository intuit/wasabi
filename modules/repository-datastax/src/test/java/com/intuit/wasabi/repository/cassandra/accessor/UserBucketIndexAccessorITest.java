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
package com.intuit.wasabi.repository.cassandra.accessor;

import static org.junit.Assert.assertEquals;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.accessor.index.UserBucketIndexAccessor;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.UUID;

/**
 * These tests are just make sure that the queries work
 */
public class UserBucketIndexAccessorITest {
    static Session session;
    static MappingManager manager;
    static UserBucketIndexAccessor accessor;
    static String applicationName = "MyTestApplication_" + System.currentTimeMillis();
	private static String bucketLabel;
	private static String context;
	private static UUID base;



    @BeforeClass
    public static void setup(){
        Injector injector = Guice.createInjector(new CassandraRepositoryModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));

        session = injector.getInstance(CassandraDriver.class).getSession();
        manager = new MappingManager(session);
        accessor = manager.createAccessor(UserBucketIndexAccessor.class);
        base = UUID.randomUUID();
        context = "context" + base;
        bucketLabel = "bucketLabel" + base;
    }

    @Test
    public void testCreateAndDelete(){   	
    	ResultSet result = accessor.countUserBy(base, context, bucketLabel);
    	long count = result.one().getLong(0);
    	assertEquals("Random count should be eq", 0, count);	
    	
    	accessor.insertBy(base, "userid1", context, new Date(), 
    			bucketLabel);

    	result = accessor.countUserBy(base, context, bucketLabel);
    	count = result.one().getLong(0);
    	assertEquals("Random count should be eq", 1, count);	

    	accessor.deleteBy(base, "userid1", context, bucketLabel);

    	result = accessor.countUserBy(base, context, bucketLabel);
    	count = result.one().getLong(0);
    	assertEquals("Random count should be eq",0, count);	
    }

    @Test
    public void testUpsertAndDeleteTwoRows(){   	
    	ResultSet result = accessor.countUserBy(base, context, bucketLabel);
    	long count = result.one().getLong(0);
    	assertEquals("Random count should be eq", 0, count);	
    	
    	accessor.insertBy(base, "userid1", context, new Date(), 
    			bucketLabel);

    	accessor.insertBy(base, "userid1", context, new Date(), 
    			bucketLabel);

    	result = accessor.countUserBy(base, context, bucketLabel);
    	count = result.one().getLong(0);
    	assertEquals("Random count should be eq", 1, count);	

    	accessor.deleteBy(base, "userid1", context, bucketLabel);

    	result = accessor.countUserBy(base, context, bucketLabel);
    	count = result.one().getLong(0);
    	assertEquals("Random count should be eq",0, count);	
    }

    @Test
    public void testInsertAndDeleteTwoRows(){   	
    	ResultSet result = accessor.countUserBy(base, context, bucketLabel);
    	long count = result.one().getLong(0);
    	assertEquals("Random count should be eq", 0, count);	
    	
    	accessor.insertBy(base, "userid1", context, new Date(), 
    			bucketLabel);

    	accessor.insertBy(base, "userid2", context, new Date(), 
    			bucketLabel);

    	result = accessor.countUserBy(base, context, bucketLabel);
    	count = result.one().getLong(0);
    	assertEquals("Random count should be eq", 2, count);	

    	accessor.deleteBy(base, "userid1", context, bucketLabel);

    	result = accessor.countUserBy(base, context, bucketLabel);
    	count = result.one().getLong(0);
    	assertEquals("Random count should be eq",1, count);	

    	accessor.deleteBy(base, "userid2", context, bucketLabel);

    	result = accessor.countUserBy(base, context, bucketLabel);
    	count = result.one().getLong(0);
    	assertEquals("Random count should be eq",0, count);	
}
}