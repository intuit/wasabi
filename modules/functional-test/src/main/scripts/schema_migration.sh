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

