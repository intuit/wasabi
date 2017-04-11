FROM openjdk:jre-alpine


ADD https://oss.sonatype.org/content/repositories/public/com/builtamont/cassandra-migration/0.9/cassandra-migration-0.9-jar-with-dependencies.jar /wasabi/cassandra-migration.jar
ADD modules/repository-datastax/src/main/resources/com/intuit/wasabi/repository/impl/cassandra/migration /wasabi/mutation
ADD bin/docker/schema_migration.sh /wasabi/migration.sh

ENTRYPOINT ["/wasabi/migration.sh"]
