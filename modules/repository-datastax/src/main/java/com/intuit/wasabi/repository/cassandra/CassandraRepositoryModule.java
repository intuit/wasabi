package com.intuit.wasabi.repository.cassandra;

import com.datastax.driver.mapping.MappingManager;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.intuit.wasabi.cassandra.datastax.CassandraDriver;
import com.intuit.wasabi.cassandra.datastax.DefaultCassandraDriver;
import com.intuit.wasabi.repository.*;
import com.intuit.wasabi.repository.cassandra.accessor.*;
import com.intuit.wasabi.repository.cassandra.accessor.audit.AuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.audit.BucketAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.audit.ExperimentAuditLogAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.count.BucketAssignmentCountAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.export.UserAssignmentExportAccessor;
import com.intuit.wasabi.repository.cassandra.accessor.index.*;
import com.intuit.wasabi.repository.cassandra.impl.*;
import com.intuit.wasabi.repository.cassandra.provider.*;
import com.intuit.wasabi.repository.cassandra.provider.audit.AuditLogAccessorProvider;
import com.intuit.wasabi.repository.cassandra.provider.audit.BucketAuditLogAccessorProvider;
import com.intuit.wasabi.repository.cassandra.provider.audit.ExperimentAuditLogAccessorProvider;
import com.intuit.wasabi.repository.cassandra.provider.count.BucketAssignmentCountAccessorProvider;
import com.intuit.wasabi.repository.cassandra.provider.export.UserAssignmentExportAccessorProvider;
import com.intuit.wasabi.repository.cassandra.provider.index.*;
import com.intuit.wasabi.userdirectory.UserDirectoryModule;
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
        install(new UserDirectoryModule());

        //Like mappers, accessors are cached at the manager level and thus, are thread-safe/sharable.
        bind(MappingManager.class).toProvider(MappingManagerProvider.class).in(Singleton.class);

        //Binding the accessors to their providers
        //NOTE: have to use provider here because the session object that is required can only be obtained by guice internally
        //using the CassandraDriver.class
        bind(UserFeedbackAccessor.class).toProvider(UserFeedbackAccessorProvider.class).in(Singleton.class);
        bind(AppRoleAccessor.class).toProvider(AppRoleAccessorProvider.class).in(Singleton.class);
        bind(UserInfoAccessor.class).toProvider(UserInfoAccessorProvider.class).in(Singleton.class);
        bind(UserRoleAccessor.class).toProvider(UserRoleAccessorProvider.class).in(Singleton.class);
        bind(ApplicationListAccessor.class).toProvider(ApplicationListAccessorProvider.class).in(Singleton.class);
        bind(ExperimentAccessor.class).toProvider(ExperimentAccessorProvider.class).in(Singleton.class);
        bind(ExperimentPageAccessor.class).toProvider(ExperimentPageAccessorProvider.class).in(Singleton.class);
        bind(ExclusionAccessor.class).toProvider(ExclusionAccessorProvider.class).in(Singleton.class);
        bind(PrioritiesAccessor.class).toProvider(PrioritiesAccessorProvider.class).in(Singleton.class);
        //Bind those indexes
        bind(AppPageIndexAccessor.class).toProvider(AppPageIndexAccessorProvider.class).in(Singleton.class);
        bind(ExperimentLabelIndexAccessor.class).toProvider(ExperimentLabelIndexAccessorProvider.class).in(Singleton.class);
        bind(ExperimentUserIndexAccessor.class).toProvider(ExperimentUserIndexAccessorProvider.class).in(Singleton.class);
        bind(PageExperimentIndexAccessor.class).toProvider(PageExperimentIndexAccessorProvider.class).in(Singleton.class);
        bind(UserAssignmentIndexAccessor.class).toProvider(UserAssignmentIndexAccessorProvider.class).in(Singleton.class);
        bind(UserBucketIndexAccessor.class).toProvider(UserBucketIndexAccessorProvider.class).in(Singleton.class);
        bind(UserExperimentIndexAccessor.class).toProvider(UserExperimentIndexAccessorProvider.class).in(Singleton.class);
        //Bind those audit
        bind(AuditLogAccessor.class).toProvider(AuditLogAccessorProvider.class).in(Singleton.class);
        bind(BucketAuditLogAccessor.class).toProvider(BucketAuditLogAccessorProvider.class).in(Singleton.class);
        bind(ExperimentAuditLogAccessor.class).toProvider(ExperimentAuditLogAccessorProvider.class).in(Singleton.class);
        //Bind those count
        bind(BucketAssignmentCountAccessor.class).toProvider(BucketAssignmentCountAccessorProvider.class).in(Singleton.class);
        //Bind those export
        bind(UserAssignmentExportAccessor.class).toProvider(UserAssignmentExportAccessorProvider.class).in(Singleton.class);
        bind(StateExperimentIndexAccessor.class).toProvider(StateExperimentIndexAccessorProvider.class).in(Singleton.class);
        bind(BucketAccessor.class).toProvider(BucketAccessorProvider.class).in(Singleton.class);

        //Bind those repositories
        bind(AuditLogRepository.class).to(CassandraAuditLogRepository.class).in(Singleton.class);
        bind(AuthorizationRepository.class).to(CassandraAuthorizationRepository.class).in(Singleton.class);
        bind(FeedbackRepository.class).to(CassandraFeedbackRepository.class).in(Singleton.class);
        bind(MutexRepository.class).to(CassandraMutexRepository.class).in(Singleton.class);
        bind(PagesRepository.class).to(CassandraPagesRepository.class).in(Singleton.class);
        bind(PrioritiesRepository.class).to(CassandraPrioritiesRepository.class).in(Singleton.class);
    }
}