#!/usr/bin/env bash

# source environment variables needed for migration tool
source /vagrant/target/scripts/cassandra_migration_rc.sh

# create keyspace
source /vagrant/target/scripts/create_keyspace.sh
# run migration tool to execute cql scripts
source /vagrant/target/scripts/schema_migration.sh