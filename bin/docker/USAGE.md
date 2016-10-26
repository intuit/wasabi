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
docker run -it --rm -e CASSANDRA_KEYSPACE_PREFIX=jabba -e CQLSH_HOST=wasabi-cassandra -e CASSANDRA_PORT=9042 --network=wasabi_nw --name wasabi_create_keyspace felixgao/wasabi_keyspace:1.0.0
```


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
 
 For example, to run the wasabi-migration container using docker, assuming the Cassandra is running on a host called wasabi-cassandra and listens on port 9042.  You may omit --network option if the cassandra node is not managed by docker.
 ```
 docker run -it --rm -e CQLSH_HOST=wasabi-cassandra -e CASSANDRA_PORT=9042 --network=wasabi_nw --name wasabi_schema_migration felixgao/wasabi-migration:1.0.0
 ```