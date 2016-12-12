package com.intuit.wasabi.data.conf.guice

import com.datastax.spark.connector.cql.{CassandraConnector, CassandraConnectorConf}
import com.google.inject.name.Named
import com.google.inject.{Inject, Provider}
import com.intuit.wasabi.data.repository.DataStoreConnectionProperties
import com.intuit.wasabi.data.util.Constants._
import org.apache.spark.{Logging, SparkConf, SparkContext}

/**
  * Created by nbarge on 10/24/16.
  */
//------ Provider for destination CassandraConnector
class CassandraConnectorProvider extends Provider[CassandraConnector] with Logging {
  var sc: SparkContext = null
  var connectionProperties: DataStoreConnectionProperties = null

  @Inject
  def this(sc: SparkContext, @Named("DataStoreConnectionProperties") connectionProperties: DataStoreConnectionProperties) {
    this
    this.sc = sc
    this.connectionProperties = connectionProperties
  }

  override def get(): CassandraConnector = {
    val host=connectionProperties.vals.get(KEY_SPARK_CASSANDRA_CONN_HOST).get
    val port=connectionProperties.vals.get(KEY_SPARK_CASSANDRA_CONN_PORT).get

    //sc.getConf.set(CassandraConnectorConf.CassandraConnectionHostProperty, host)
    //sc.getConf.set(CassandraConnectorConf.CassandraConnectionPortProperty, port)
    val conn = CassandraConnector(sc.getConf)

    //Test connection
    //if(log.isInfoEnabled) log.info(s"Testing connection for $host:$port")

    val session = conn.openSession()
    val cluster = session.getCluster
    val metadata = cluster.getMetadata
    if(log.isInfoEnabled) log.info(s"Cluster: ${cluster.getClusterName}")

    val itr = metadata.getAllHosts().iterator()
    while(itr.hasNext) {
      val host = itr.next
      if(log.isInfoEnabled) log.info(s"Datatacenter: ${host.getDatacenter}; Host: ${host.getAddress}; Rack: ${host.getRack}")
    }
    session.close()

    conn
  }
}
