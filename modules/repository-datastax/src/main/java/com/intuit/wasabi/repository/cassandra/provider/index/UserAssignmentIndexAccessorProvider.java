package com.intuit.wasabi.repository.cassandra.provider.index;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.index.UserAssignmentIndexAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserAssignmentIndexAccessorProvider implements Provider<UserAssignmentIndexAccessor> {
    private final Logger logger = LoggerFactory.getLogger(UserAssignmentIndexAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public UserAssignmentIndexAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public UserAssignmentIndexAccessor get() {
        return manager.createAccessor(UserAssignmentIndexAccessor.class);
    }
}