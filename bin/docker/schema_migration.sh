#!/usr/bin/env sh

echo "java -jar -Dcassandra.migration.keyspace.name=${CASSANDRA_KEYSPACE:-wasabi_experiments} \
    -Dcassandra.migration.cluster.port=${CASSANDRA_PORT:-9042} \
    -Dcassandra.migration.cluster.username=${CQLSH_USERNAME} \
    -Dcassandra.migration.cluster.password=${CQLSH_PASSWORD} \
    -Dcassandra.migration.scripts.locations=filesystem:${MIGRATION_SCRIPT:-/wasabi/mutation}\
    -Dcassandra.migration.cluster.contactpoints=${CQLSH_HOST:-localhost} \
    ${CASSANDRA_MIGRATION:-/wasabi/cassandra-migration.jar} migrate"

while ! nc -w 1 -z ${CQLSH_HOST:-localhost} ${CASSANDRA_PORT:-9042}; do sleep 0.1; done

java -jar -Dcassandra.migration.keyspace.name=${CASSANDRA_KEYSPACE:-wasabi_experiments} \
    -Dcassandra.migration.cluster.port=${CASSANDRA_PORT:-9042} \
    -Dcassandra.migration.cluster.username=${CQLSH_USERNAME} \
    -Dcassandra.migration.cluster.password=${CQLSH_PASSWORD} \
    -Dcassandra.migration.scripts.locations=filesystem:${MIGRATION_SCRIPT:-/wasabi/mutation} \
    -Dcassandra.migration.cluster.contactpoints=${CQLSH_HOST:-localhost} \
    ${CASSANDRA_MIGRATION:-/wasabi/cassandra-migration.jar} migrate

if [ $? -ne 0 ]; then
    echo "failed to execute the migration script. Please contact administrator."
    exit 1;
fi
