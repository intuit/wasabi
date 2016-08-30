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

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.datastax.driver.mapping.Result;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.CassandraRepositoryModule;
import com.intuit.wasabi.repository.cassandra.pojo.ApplicationList;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

/**
 * These tests are just make sure that the queries work
 */
public class ApplicationListAccessorITest {
    static Session session;
    static MappingManager manager;
    static ApplicationListAccessor accessor;
    static String applicationName = "MyTestApplication_" + System.currentTimeMillis();

    @BeforeClass
    public static void setup(){
        Injector injector = Guice.createInjector(new CassandraRepositoryModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));

        session = injector.getInstance(CassandraDriver.class).getSession();
        manager = new MappingManager(session);
        accessor = manager.createAccessor(ApplicationListAccessor.class);
    }

    @Test
    public void testCreateAndDelete(){
    	Result<ApplicationList> appNames = accessor.getUniqueAppName();
    	List<ApplicationList> listOfAppNames = appNames.all();
    	int count = listOfAppNames.size();

    	accessor.insert(applicationName);
    	appNames = accessor.getUniqueAppName();
    	listOfAppNames = appNames.all();
    	assertEquals("Counnt should be equal", count + 1, listOfAppNames.size());
    	
    	accessor.deleteBy(applicationName);
    	appNames = accessor.getUniqueAppName();
    	listOfAppNames = appNames.all();
    	assertEquals("Counnt should be equal", count, listOfAppNames.size());
    	
    }

}