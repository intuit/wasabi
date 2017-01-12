package com.intuit.wasabi.repository.cassandra.provider.index;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.index.ExperimentLabelIndexAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExperimentLabelIndexAccessorProvider implements Provider<ExperimentLabelIndexAccessor> {
    private final Logger logger = LoggerFactory.getLogger(ExperimentLabelIndexAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public ExperimentLabelIndexAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public ExperimentLabelIndexAccessor get() {
        return manager.createAccessor(ExperimentLabelIndexAccessor.class);
    }
}