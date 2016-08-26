package com.intuit.wasabi.repository.cassandra.provider;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.UserAssignmentAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserAssignmentAccessorProvider implements Provider<UserAssignmentAccessor> {
    private final Logger logger = LoggerFactory.getLogger(UserAssignmentAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public UserAssignmentAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public UserAssignmentAccessor get() {
        return manager.createAccessor(UserAssignmentAccessor.class);
    }
}