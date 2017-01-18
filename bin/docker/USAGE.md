This directory contains two docker files
1. cqlsh.docker
2. migration.docker

##Create Keyspace
To create the docker images that is needed for creating keyspace, you may use the following command.

```
docker build -t felixgao/wasabi_keyspace:1.0.0 -f cqlsh.docker .
```

####Environment Variables
- CASSANDRA_KEYSPACE_PREFIX
    * default: wasabi
- CQLSH_USERNAME
    * default: [empty]
- CQLSH_PASSWORD
    * default: [empty]
- CQLSH_HOST
    * default: localhost
- CASSANDRA_REPLICATION
    * default: 1
- CASSANDRA_PORT
    * default: 9042

For example, to run the cqlsh container using docker, assuming the Cassandra is running on a host called wasabi-cassandra and listens on port 9042.  You may omit --network option if the cassandra node is not managed by docker.
```
docker run -it --rm -e CASSANDRA_KEYSPACE_PREFIX=wasabi -e CQLSH_HOST=wasabi-cassandra -e CASSANDRA_PORT=9042 --network=wasabi_nw --name wasabi_create_keyspace felixgao/wasabi_keyspace:1.0.0
```

## Important Notes:
- The migration scripts do not save and restore data in the tables being modified.
- The migration cql script names indicate the nature of the mutation involved in that step - eg: Create, Drop, etc
- Destructive cql that have ```Drop``` in the name will drop the table and data in that table will be lost. If such data is required, please save it before executing the migration script and reimport it into the new table as required.

##Migration

 To create the docker image that is need for migration, you may use the following command
 ```
 docker build -t felixgao/wasabi-migration:1.0.0 -f migration.docker ../../
 ```
 The above command assumes you are in the $ProjectDir/bin/docker directory.  You may change the docker build context as you see fit.

 ####Environment Variables
 - CASSANDRA_KEYSPACE
     * default: wasabi_experiments
 - CQLSH_USERNAME
     * default: [empty]
 - CQLSH_PASSWORD
     * default: [empty]
 - CQLSH_HOST
     * default: localhost
 - MIGRATION_SCRIPT
     * default: /wasabi/mutation
 - CASSANDRA_PORT
     * default: 9042
 - CASSANDRA_MIGRATION
     # default: /wasabi/cassandra-migration.jar

 For example, to run the wasabi-migration container using docker, assuming the Cassandra is running on a host called wasabi-cassandra and listens on port 9042.  You may omit --network option if the cassandra node is not managed by docker.
 ```
 docker run -it --rm -e CQLSH_HOST=wasabi-cassandra -e CASSANDRA_PORT=9042 --network=wasabi_nw --name wasabi_schema_migration felixgao/wasabi-migration:1.0.0
 ```

## Local Cassandra

if you are running the migration against local installation on the mac or your ip is not publicly reachable.  You will have to ran the script manually by providing the above environment variables using the ```migration.sh``` directly.

for example, if your Mac OSx is running the cassandra locally, you will need the following command to migration.

```
export CASSANDRA_MIGRATION=/local_location_of_the_migration_tool.jar
export MIGRATION_SCRIPT=/location_of_where_the_cql_scripts
bin/docker/migration.sh
```
