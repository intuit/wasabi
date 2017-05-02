#!/usr/bin/env sh

echo "applying open source migration scripts"
echo "java -jar -Dcassandra.migration.keyspace.name=${CASSANDRA_KEYSPACE} \
    -Dcassandra.migration.cluster.port=${CASSANDRA_PORT:-9042} \
    -Dcassandra.migration.cluster.username=${CQLSH_USERNAME} \
    -Dcassandra.migration.cluster.password=${CQLSH_PASSWORD} \
    -Dcassandra.migration.scripts.locations=filesystem:${MIGRATION_SCRIPT_1} \
    -Dcassandra.migration.cluster.contactpoints=${CQLSH_HOST:-localhost} \
    -Dcassandra.migration.table.prefix=wasabi_ \
    ${CASSANDRA_MIGRATION} migrate"

java -jar -Dcassandra.migration.keyspace.name=${CASSANDRA_KEYSPACE} \
    -Dcassandra.migration.cluster.port=${CASSANDRA_PORT:-9042} \
    -Dcassandra.migration.cluster.username=${CQLSH_USERNAME} \
    -Dcassandra.migration.cluster.password=${CQLSH_PASSWORD} \
    -Dcassandra.migration.scripts.locations=filesystem:${MIGRATION_SCRIPT_1} \
    -Dcassandra.migration.cluster.contactpoints=${CQLSH_HOST:-localhost} \
    -Dcassandra.migration.table.prefix=wasabi_ \
    ${CASSANDRA_MIGRATION} migrate

if [ $? -ne 0 ]; then
    echo "failed to execute the oss migration script. Please contact administrator."
    exit 1;
fi

