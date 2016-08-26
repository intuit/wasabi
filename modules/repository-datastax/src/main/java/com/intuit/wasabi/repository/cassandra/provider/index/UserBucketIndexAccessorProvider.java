package com.intuit.wasabi.repository.cassandra.provider.index;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.index.UserBucketIndexAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserBucketIndexAccessorProvider implements Provider<UserBucketIndexAccessor> {
    private final Logger logger = LoggerFactory.getLogger(UserBucketIndexAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public UserBucketIndexAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public UserBucketIndexAccessor get() {
        return manager.createAccessor(UserBucketIndexAccessor.class);
    }
}