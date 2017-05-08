###############################################################################
# Copyright 2017 Intuit
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
#!/usr/bin/env bash

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