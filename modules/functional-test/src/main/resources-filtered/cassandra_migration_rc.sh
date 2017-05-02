#!/bin/bash

export CASSANDRA_KEYSPACE=${cassandra.experiments.keyspaceName}
export CASSANDRA_KEYSPACE_PREFIX=${cassandra.keyspace.prefix}

export MIGRATION_SCRIPT_1=/vagrant/target/mutation/${wasabi.cassandra.migration.resource.path}
export CASSANDRA_MIGRATION=/vagrant/target/cassandra-migration-${cassandra.migration.version}-jar-with-dependencies.jar
export CQLSH_HOST=localhost
export CASSANDRA_REPLICATION=1

echo CASSANDRA_KEYSPACE $CASSANDRA_KEYSPACE
echo MIGRATION_SCRIPT_1 $MIGRATION_SCRIPT_1
echo CASSANDRA_MIGRATION $CASSANDRA_MIGRATION
echo CASSANDRA_KEYSPACE_PREFIX $CASSANDRA_KEYSPACE_PREFIX
echo CQLSH_HOST $CQLSH_HOST
echo CASSANDRA_REPLICATION $CASSANDRA_REPLICATION
