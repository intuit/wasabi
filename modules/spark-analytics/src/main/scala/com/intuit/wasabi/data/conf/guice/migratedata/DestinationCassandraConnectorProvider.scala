package com.intuit.wasabi.data.conf.guice.migratedata

import com.datastax.spark.connector.cql.CassandraConnector
import com.google.inject.name.Named
import com.google.inject.{Inject, Provider}
import com.intuit.wasabi.data.repository.DataStoreConnectionProperties
import com.intuit.wasabi.data.util.Constants._
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by nbarge on 10/24/16.
  */
//------ Provider for destination CassandraConnector
class DestinationCassandraConnectorProvider extends Provider[CassandraConnector] {
  var sc: SparkContext = null
  var connectionProperties: DataStoreConnectionProperties = null

  @Inject
  def this(sc: SparkContext, @Named("DestinationDataStoreConnectionProperties") connectionProperties: DataStoreConnectionProperties) {
    this
    this.sc = sc
    this.connectionProperties = connectionProperties
  }

  override def get(): CassandraConnector = {
    val conf = new SparkConf()
    conf.setAll(sc.getConf.getAll)
    conf.set(KEY_SPARK_CASSANDRA_CONN_HOST, connectionProperties.vals.get(KEY_SPARK_CASSANDRA_CONN_HOST).get)
    conf.set(KEY_SPARK_CASSANDRA_CONN_PORT, connectionProperties.vals.get(KEY_SPARK_CASSANDRA_CONN_PORT).get)
    CassandraConnector(conf)
  }
}
