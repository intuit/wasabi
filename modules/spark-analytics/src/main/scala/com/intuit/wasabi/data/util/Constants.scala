package com.intuit.wasabi.data.util

import java.time.ZoneId

/**
  * Central place to define Constants
  *
  */
object Constants {
  val DATASTORE_CASSANDRA="cassandra"
  val DATASTORE_MYSQL="mysql"

  val KEY_SPARK_CASSANDRA_CONN_HOST = "spark.cassandra.connection.host"
  val KEY_SPARK_CASSANDRA_CONN_PORT = "spark.cassandra.connection.port"
  val KEY_SPARK_CASSANDRA_CONN_KEYSPACE = "spark.cassandra.connection.keyspace"
  val KEY_SPARK_CASSANDRA_CONN_CLUSTER = "spark.cassandra.connection.cluster"

  val PERFORMANCE_LOGGER = "PERF_LOGGER"

  val APP_ARG_APP_ID = "app_id"
  val APP_ARG_MASTER = "master"

  val SPARK_SQL_CASSANDRA_FORMAT = "org.apache.spark.sql.cassandra"

  val APP_ID_MIGRATE_DATA = "migrate-data"
  val APP_ID_DAILY_AGGREGATION = "migrate-data"
  val APP_ID_REAL_TIME = "migrate-data"

  val DEFAULT_CLUSTER_NAME = "default"
  val DEFAULT_ZONE = ZoneId.of("America/Los_Angeles")
}
