package com.intuit.wasabi.repository.cassandra.provider.index;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentUserIndexAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExperimentUserIndexAccessorProvider implements Provider<ExperimentUserIndexAccessor> {
    private final Logger logger = LoggerFactory.getLogger(ExperimentUserIndexAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public ExperimentUserIndexAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public ExperimentUserIndexAccessor get() {
        return manager.createAccessor(ExperimentUserIndexAccessor.class);
    }
}