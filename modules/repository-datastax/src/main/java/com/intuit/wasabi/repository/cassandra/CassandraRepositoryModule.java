package com.intuit.wasabi.repository.cassandra;

import com.datastax.driver.mapping.MappingManager;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.cassandra.datastax.DefaultCassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.UserFeedbackAccessor;
import com.intuit.wasabi.repository.cassandra.pojo.UserFeedback;

import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class CassandraRepositoryModule extends AbstractModule {
    public static final String PROPERTY_NAME = "/cassandra_client_config.properties";
    private static final Logger LOGGER = getLogger(CassandraRepositoryModule.class);

    @Override
    protected void configure() {
        ClientConfiguration config = new ClientConfiguration(PROPERTY_NAME);
        bind(CassandraDriver.Configuration.class).toInstance(config);
        bind(String.class).annotatedWith(Names.named("CassandraInstanceName")).toInstance("CassandraWasabiCluster");
        bind(CassandraDriver.class).to(DefaultCassandraDriver.class).asEagerSingleton();
    }
}