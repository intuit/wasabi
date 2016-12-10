package com.intuit.wasabi.data.conf.guice.migratedata

import com.google.inject.name.Named
import com.google.inject.{Inject, Provider}
import com.intuit.wasabi.data.repository.DataStoreConnectionProperties
import com.intuit.wasabi.data.util.Constants._
import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.apache.spark.{Logging, SparkContext}

/**
  * Created by nbarge on 10/24/16.
  */
//------ Provider for SQLContext
class DestinationCassandraSQLContextProvider extends Provider[CassandraSQLContext] with Logging {
  var sc: SparkContext = null
  var connectionProperties: DataStoreConnectionProperties = null

  @Inject
  def this(sc: SparkContext, @Named("DestinationDataStoreConnectionProperties") connectionProperties: DataStoreConnectionProperties) {
    this
    this.sc=sc
    this.connectionProperties=connectionProperties
  }

  override def get = {
    val sqlContext = new CassandraSQLContext(sc)
    val dCluster=connectionProperties.vals.get(KEY_SPARK_CASSANDRA_CONN_CLUSTER).get
    val dHost=connectionProperties.vals.get(KEY_SPARK_CASSANDRA_CONN_HOST).get
    val dPort=connectionProperties.vals.get(KEY_SPARK_CASSANDRA_CONN_PORT).get
    if(log.isInfoEnabled) log.info(s"dCluster=$dCluster, dHost=$dHost, dPort=$dPort")

    //sqlContext.setConf(s"$dCluster/spark.cassandra.connection.host", dHost)

    sqlContext
  }
}
