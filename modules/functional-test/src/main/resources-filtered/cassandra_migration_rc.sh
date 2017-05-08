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
