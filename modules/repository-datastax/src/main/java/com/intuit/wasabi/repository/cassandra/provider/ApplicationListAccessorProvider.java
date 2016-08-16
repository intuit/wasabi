package com.intuit.wasabi.repository.cassandra.provider;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.ApplicationListAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationListAccessorProvider implements Provider<ApplicationListAccessor> {
    private final Logger logger = LoggerFactory.getLogger(ApplicationListAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public ApplicationListAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public ApplicationListAccessor get() {
        return manager.createAccessor(ApplicationListAccessor.class);
    }
}