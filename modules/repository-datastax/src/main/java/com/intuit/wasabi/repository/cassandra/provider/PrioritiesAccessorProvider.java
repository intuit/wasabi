package com.intuit.wasabi.repository.cassandra.provider;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.PrioritiesAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrioritiesAccessorProvider implements Provider<PrioritiesAccessor> {
    private final Logger logger = LoggerFactory.getLogger(PrioritiesAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public PrioritiesAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public PrioritiesAccessor get() {
        return manager.createAccessor(PrioritiesAccessor.class);
    }
}