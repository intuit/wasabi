package com.intuit.wasabi.repository.cassandra.provider.export;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.export.UserAssignmentExportAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserAssignmentExportAccessorProvider implements Provider<UserAssignmentExportAccessor> {
    private final Logger logger = LoggerFactory.getLogger(UserAssignmentExportAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public UserAssignmentExportAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public UserAssignmentExportAccessor get() {
        return manager.createAccessor(UserAssignmentExportAccessor.class);
    }
}