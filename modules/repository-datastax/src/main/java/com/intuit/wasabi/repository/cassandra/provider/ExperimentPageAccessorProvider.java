package com.intuit.wasabi.repository.cassandra.provider;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.ExperimentPageAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExperimentPageAccessorProvider implements Provider<ExperimentPageAccessor> {
    private final Logger logger = LoggerFactory.getLogger(ExperimentPageAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public ExperimentPageAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public ExperimentPageAccessor get() {
        return manager.createAccessor(ExperimentPageAccessor.class);
    }
}