package com.intuit.wasabi.repository.cassandra.provider;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.UserFeedbackAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserFeedbackAccessorProvider implements Provider<UserFeedbackAccessor> {
    private final Logger logger = LoggerFactory.getLogger(UserFeedbackAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public UserFeedbackAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public UserFeedbackAccessor get() {
        return manager.createAccessor(UserFeedbackAccessor.class);
    }
}