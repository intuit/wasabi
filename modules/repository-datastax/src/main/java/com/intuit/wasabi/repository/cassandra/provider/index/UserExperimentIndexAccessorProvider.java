package com.intuit.wasabi.repository.cassandra.provider.index;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.index.UserExperimentIndexAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserExperimentIndexAccessorProvider implements Provider<UserExperimentIndexAccessor> {
    private final Logger logger = LoggerFactory.getLogger(UserExperimentIndexAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public UserExperimentIndexAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public UserExperimentIndexAccessor get() {
        return manager.createAccessor(UserExperimentIndexAccessor.class);
    }
}