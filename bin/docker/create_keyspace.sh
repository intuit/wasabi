#!/usr/bin/env sh

# The cqlsh is a duplicate of the the existing cqlsh==5.0.3 with hard coded default protocol version to 3.
# the original cqlsh have hard coded protocol version of 4.
# the current [2016-10-01] version of server spec is [Cassandra 2.1.15 | CQL spec 3.2.1 | Native protocol v3]
echo "cqlsh " \
     "--cqlversion=\"${CQLSH_VERSION:-3.2.1}\" "\
     "-e \"CREATE KEYSPACE IF NOT EXISTS ${CASSANDRA_KEYSPACE_PREFIX:-wasabi}_experiments WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : ${CASSANDRA_REPLICATION:-1}};\"" \
     "--username=${CQLSH_USERNAME}" \
     "--password=\"${CQLSH_PASSWORD}\"" \
     "${CQLSH_HOST:-localhost}" \
     "${CASSANDRA_PORT:-9042}"

while ! nc -w 1 -z ${CQLSH_HOST:-localhost} ${CASSANDRA_PORT:-9042}; do sleep 0.1; done

cqlsh --cqlversion="${CQLSH_VERSION:-3.2.1}" \
    -e "CREATE KEYSPACE IF NOT EXISTS ${CASSANDRA_KEYSPACE_PREFIX:-wasabi}_experiments WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : ${CASSANDRA_REPLICATION:-1}};" \
    --username=${CQLSH_USERNAME} \
    --password="${CQLSH_PASSWORD}" \
    ${CQLSH_HOST:-localhost} \
    ${CASSANDRA_PORT:-9042}

if [ $? -ne 0 ]; then
    echo "failed to execute the create keyspace command. Please contact administrator."
    exit 1;
fi
