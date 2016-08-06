package com.intuit.wasabi.repository.cassandra;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class CassandraRepositoryModuleTest {
    private final Logger logger = LoggerFactory.getLogger(CassandraRepositoryModuleTest.class);

    @Test
    public void testConfigure() throws Exception {
        Injector injector = Guice.createInjector(new CassandraRepositoryModule());
        injector.getInstance(Key.get(String.class, Names.named("CassandraInstanceName")));

        assertThat(injector.getInstance(CassandraDriver.class), is(not(nullValue())));
        assertThat(injector.getInstance(CassandraDriver.class).isKeyspaceInitialized(), is(true));
    }
}