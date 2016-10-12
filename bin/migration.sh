#!/usr/bin/env bash
###############################################################################
# Copyright 2016 Intuit
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
###############################################################################
###############################################################################
# This script can be used to work with remote cassandra schema migration.  In order,
# to do that, you will need to set the following environment variables.
# CQLSH_USERNAME: the username to connect to remote cluster. [Optional], default to empty
# CQLSH_PASSWORD: the password to connect to remote cluster. [Optional]. default to empty
# CQLSH_HOST: the contact point for remote cluster. [Optional]. default to localhost
# CQL_MIGRATION_SCRIPTS: the directory contains all cql migration scripts. [Optional]. default to $PROEJCT_HOME/modules/repository-datastax/db/mutation/
# CASSANDRA_REPLICATION: the replication factor for cassandra keyspace. [Optional]. default to 1.
###############################################################################

#Get the current location of the script
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do # resolve $SOURCE until the file is no longer a symlink
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

#Create keyspace if it is not created in cassandra
CONTAINER_NAME=${CASSANDRA_CONTAINER:-wasabi-cassandra}
IS_CONTAINER=`docker inspect -f {{.State.Running}} $CONTAINER_NAME`

if [ "$IS_CONTAINER" = true ] ; then
    echo 'Since we are in a container'
    echo "docker exec -it ${CONTAINER_NAME} \"cqlsh -e \"CREATE KEYSPACE IF NOT EXISTS wasabi_experiments WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : ${CASSANDRA_REPLICATION:-1}};\" --username=${CQLSH_USERNAME} --password=\"${CQLSH_PASSWORD}\" ${CQLSH_HOST:-localhost} ${CASSANDRA_PORT:-9042}\""
    docker exec -it ${CONTAINER_NAME} cqlsh -e "CREATE KEYSPACE IF NOT EXISTS wasabi_experiments WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : ${CASSANDRA_REPLICATION:-1}};" --username=${CQLSH_USERNAME} --password="${CQLSH_PASSWORD}" ${CQLSH_HOST:-localhost} ${CASSANDRA_PORT:-9042}
else
    echo 'Not in container, assuming cassandra is up and running as a process'
    echo "cqlsh -e \"CREATE KEYSPACE IF NOT EXISTS wasabi_experiments WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : ${CASSANDRA_REPLICATION:-1}};\" --username=${CQLSH_USERNAME} --password=\"${CQLSH_PASSWORD}\" ${CQLSH_HOST:-localhost} ${CASSANDRA_PORT:-9042}"
    cqlsh -e "CREATE KEYSPACE IF NOT EXISTS wasabi_experiments WITH replication = {'class' : 'SimpleStrategy', 'replication_factor' : ${CASSANDRA_REPLICATION:-1}};" --username=${CQLSH_USERNAME} --password="${CQLSH_PASSWORD}" ${CQLSH_HOST:-localhost} ${CASSANDRA_PORT:-9042}
fi

if [ $? -ne 0 ]; then
    echo "failed to execute the create keyspace command. Please contact administrator."
    exit 1;
fi

#Download Cassandra Migration tool
filename="cassandra-migration-0.9-20160930.013737-27-jar-with-dependencies.jar"
#Sanpshot can be used if you want to latest features
#URL="https://oss.sonatype.org/content/repositories/snapshots/com/builtamont/cassandra-migration/0.9-SNAPSHOT/$filename"
URL="https://oss.sonatype.org/content/repositories/public/com/builtamont/cassandra-migration/0.9-SNAPSHOT/$filename"
scratch=$(mktemp -d -t tmp.cassandra-migration)
function finish {
  rm -rf "$scratch"
}
curl -q $URL -o "$scratch/$filename" || true
if [ -f "$scratch/$filename" ]; then
    echo "$scratch/$filename"
fi
trap finish EXIT

#Execute the migration scripts
MIGRATION_SCRIPT=${CQL_MIGRATION_SCRIPTS:-"$DIR/../modules/repository-datastax/db/mutation/"}
echo "java -jar -Dcassandra.migration.keyspace.name=wasabi_experiments \
    -Dcassandra.migration.cluster.port=${CASSANDRA_PORT:-9042} \
    -Dcassandra.migration.cluster.username=${CQLSH_USERNAME} \
    -Dcassandra.migration.cluster.password=${CQLSH_PASSWORD} \
    -Dcassandra.migration.scripts.locations=filesystem:${MIGRATION_SCRIPT}\
    -Dcassandra.migration.cluster.contactpoints=${CQLSH_HOST:-localhost} \
    \"$scratch/$filename\" migrate"

java -jar -Dcassandra.migration.keyspace.name=wasabi_experiments \
    -Dcassandra.migration.cluster.port=${CASSANDRA_PORT:-9042} \
    -Dcassandra.migration.cluster.username=${CQLSH_USERNAME} \
    -Dcassandra.migration.cluster.password=${CQLSH_PASSWORD} \
    -Dcassandra.migration.scripts.locations=filesystem:${MIGRATION_SCRIPT} \
    -Dcassandra.migration.cluster.contactpoints=${CQLSH_HOST:-localhost} \
    "$scratch/$filename" migrate

if [ $? -ne 0 ]; then
    echo "failed to execute the migration script. Please contact administrator."
    exit 1;
fi