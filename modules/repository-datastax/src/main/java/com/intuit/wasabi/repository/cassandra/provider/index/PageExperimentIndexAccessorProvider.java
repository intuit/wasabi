package com.intuit.wasabi.repository.cassandra.provider.index;

import com.datastax.driver.core.Session;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.index.PageExperimentIndexAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageExperimentIndexAccessorProvider implements Provider<PageExperimentIndexAccessor> {
    private final Logger logger = LoggerFactory.getLogger(PageExperimentIndexAccessorProvider.class);
    private final Session session;
    private final MappingManager manager;

    @Inject
    public PageExperimentIndexAccessorProvider(CassandraDriver driver) {
        this.session = driver.getSession();
        this.manager = new MappingManager(session);
    }


    @Override
    public PageExperimentIndexAccessor get() {
        return manager.createAccessor(PageExperimentIndexAccessor.class);
    }
}