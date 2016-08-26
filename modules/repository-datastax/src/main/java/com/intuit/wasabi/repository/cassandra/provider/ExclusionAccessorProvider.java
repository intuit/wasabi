package com.intuit.wasabi.repository.cassandra.provider;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.ExclusionAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExclusionAccessorProvider implements Provider<ExclusionAccessor> {
    private final Logger logger = LoggerFactory.getLogger(ExclusionAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public ExclusionAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public ExclusionAccessor get() {
        return manager.createAccessor(ExclusionAccessor.class);
    }
}