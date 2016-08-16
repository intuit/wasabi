package com.intuit.wasabi.repository.cassandra;

import com.datastax.driver.mapping.MappingManager;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.cassandra.datastax.DefaultCassandraDriver;
import com.intuit.wasabi.repository.cassandra.accessor.*;
import com.intuit.wasabi.repository.cassandra.provider.*;
import org.slf4j.Logger;

import javax.inject.Singleton;

import static org.slf4j.LoggerFactory.getLogger;

public class CassandraRepositoryModule extends AbstractModule {
    public static final String PROPERTY_NAME = "/cassandra_client_config.properties";
    private static final Logger LOGGER = getLogger(CassandraRepositoryModule.class);

    @Override
    protected void configure() {
        ClientConfiguration config = new ClientConfiguration(PROPERTY_NAME);
        bind(CassandraDriver.Configuration.class).toInstance(config);
        bind(String.class).annotatedWith(Names.named("CassandraInstanceName")).toInstance("CassandraWasabiCluster");
        bind(CassandraDriver.class).to(DefaultCassandraDriver.class).asEagerSingleton();

        //Binding the accessors to their providers
        //NOTE: have to use provider here because the session object that is required can only be obtained by guice internally
        //using the CassandraDriver.class
        //Like mappers, accessors are cached at the manager level and thus, are thread-safe/sharable.
        bind(MappingManager.class).toProvider(MappingManagerProvider.class).in(Singleton.class);
        bind(UserFeedbackAccessor.class).toProvider(UserFeedbackAccessorProvider.class).in(Singleton.class);
        bind(AppRoleAccessor.class).toProvider(AppRoleAccessorProvider.class).in(Singleton.class);
        bind(UserInfoAccessor.class).toProvider(UserInfoAccessorProvider.class).in(Singleton.class);
        bind(UserRoleAccessor.class).toProvider(UserRoleAccessorProvider.class).in(Singleton.class);
        bind(ApplicationListAccessor.class).toProvider(ApplicationListAccessorProvider.class).in(Singleton.class);
    }
}