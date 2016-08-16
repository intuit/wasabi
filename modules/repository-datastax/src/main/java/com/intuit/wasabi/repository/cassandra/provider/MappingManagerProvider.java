package com.intuit.wasabi.repository.cassandra.provider;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingManagerProvider implements Provider<MappingManager> {
    private final Logger logger = LoggerFactory.getLogger(MappingManagerProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public MappingManagerProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public MappingManager get() {
        return this.manager;
    }
}