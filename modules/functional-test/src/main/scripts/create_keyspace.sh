#!/usr/bin/env sh

echo "Creating Keyspace ${CASSANDRA_KEYSPACE_PREFIX}_experiments if it not exits"
echo "cqlsh " \
     "-e \"CREATE KEYSPACE IF NOT EXISTS ${CASSANDRA_KEYSPACE_PREFIX}_experiments WITH replication = {'class' : 'NetworkTopologyStrategy', 'datacenter1' : ${CASSANDRA_REPLICATION:-1}};\"" \
     "--username=${CQLSH_USERNAME}" \
     "--password=\"${CQLSH_PASSWORD}\"" \
     "${CQLSH_HOST:-localhost}" \
     "${CASSANDRA_PORT:-9042}"

cqlsh \
    -e "CREATE KEYSPACE IF NOT EXISTS ${CASSANDRA_KEYSPACE_PREFIX}_experiments WITH replication = {'class' : 'NetworkTopologyStrategy', 'datacenter1' : ${CASSANDRA_REPLICATION:-1}};" \
    --username=${CQLSH_USERNAME} \
    --password="${CQLSH_PASSWORD}" \
    ${CQLSH_HOST:-localhost} \
    ${CASSANDRA_PORT:-9042}


if [ $? -ne 0 ]; then
    echo "failed to execute the create keyspace command. Please contact administrator."
    exit 1;
fi

echo "Done creating keyspace"