package com.intuit.wasabi.data.conf.guice.migratedata

import com.google.inject.name.Named
import com.google.inject.{Inject, Provider}
import com.intuit.wasabi.data.repository.DataStoreConnectionProperties
import com.intuit.wasabi.data.util.Constants._
import org.apache.spark.{Logging, SparkContext}
import org.apache.spark.sql.cassandra.CassandraSQLContext

/**
  * Created by nbarge on 10/24/16.
  */
//------ Provider for SQLContext
class SourceCassandraSQLContextProvider extends Provider[CassandraSQLContext] with Logging {
  var sc: SparkContext = null
  var connectionProperties: DataStoreConnectionProperties = null

  @Inject
  def this(sc: SparkContext, @Named("SourceDataStoreConnectionProperties") connectionProperties: DataStoreConnectionProperties) {
    this
    this.sc=sc
    this.connectionProperties=connectionProperties
  }

  override def get = {
    val sqlContext = new CassandraSQLContext(sc)
    val sCluster=connectionProperties.vals.get(KEY_SPARK_CASSANDRA_CONN_CLUSTER).get
    val sHost=connectionProperties.vals.get(KEY_SPARK_CASSANDRA_CONN_HOST).get
    val sPort=connectionProperties.vals.get(KEY_SPARK_CASSANDRA_CONN_PORT).get
    if(log.isInfoEnabled) log.info(s"sCluster=$sCluster, sHost=$sHost, sPort=$sPort")

    //sqlContext.setConf(s"$sCluster/spark.cassandra.connection.host", sHost)

    sqlContext
  }
}
