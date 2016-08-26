package com.intuit.wasabi.repository.cassandra.provider.count;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.count.BucketAssignmentCountAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BucketAssignmentCountAccessorProvider implements Provider<BucketAssignmentCountAccessor> {
    private final Logger logger = LoggerFactory.getLogger(BucketAssignmentCountAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public BucketAssignmentCountAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public BucketAssignmentCountAccessor get() {
        return manager.createAccessor(BucketAssignmentCountAccessor.class);
    }
}