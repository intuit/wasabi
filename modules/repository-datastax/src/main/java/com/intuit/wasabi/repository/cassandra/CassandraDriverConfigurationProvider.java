package com.intuit.wasabi.repository.cassandra;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraDriverConfigurationProvider implements Provider<CassandraDriver.Configuration> {
    private final Logger logger = LoggerFactory.getLogger(CassandraDriverConfigurationProvider.class);
    private final ClientConfiguration config;

    @Inject
    public CassandraDriverConfigurationProvider(@Named("cassandraClientConfig") String path) {
        this.config = new ClientConfiguration(path);
    }


    @Override
    public CassandraDriver.Configuration get() {
        return config;
    }
}