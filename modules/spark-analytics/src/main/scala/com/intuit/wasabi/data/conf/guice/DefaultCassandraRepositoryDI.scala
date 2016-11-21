package com.intuit.wasabi.data.conf.guice

import com.datastax.spark.connector.cql.CassandraConnector
import com.google.inject.name.Names
import com.intuit.wasabi.data.repository.{DataStoreConnectionProperties, SparkDataStoreRepository}
import com.intuit.wasabi.data.repository.cassandra.SparkCassandraRepository
import com.intuit.wasabi.data.util.Constants._
import com.intuit.wasabi.data.util.Utility
import com.typesafe.config.Config
import org.apache.spark.SparkContext
import org.apache.spark.sql.cassandra.CassandraSQLContext

/**
  * Created by nbarge on 11/3/16.
  */
class DefaultCassandraRepositoryDI(appConfig: Config, sc: Option[SparkContext] = None) extends CommonSparkApplicationDI(appConfig, sc) {

  override def configure(): Unit = {
    super.configure()

    val mConfig = Utility.configToMap(appConfig.getConfig("datastore"))

    //Read DataStore configs
    val dType = mConfig.get("type").get
    val dCluster = mConfig.get("cluster").get
    val dHost = mConfig.get("host").get
    val dPort = mConfig.get("port").get
    val dKeyspace = mConfig.get("keyspace").get

    //Create & bind destination DataStoreConnectionProperties
    bind(classOf[DataStoreConnectionProperties]).annotatedWith(Names.named("DataStoreConnectionProperties")).toInstance(new DataStoreConnectionProperties(Map(KEY_SPARK_CASSANDRA_CONN_CLUSTER -> dCluster, KEY_SPARK_CASSANDRA_CONN_HOST -> dHost, KEY_SPARK_CASSANDRA_CONN_PORT -> dPort, KEY_SPARK_CASSANDRA_CONN_KEYSPACE -> dKeyspace)))

    //-- Bind destination CassandraConnector provider
    bind(classOf[CassandraConnector]).annotatedWith(Names.named("DataStoreCassandraConnector")).toProvider(classOf[CassandraConnectorProvider])

    //-- Bind destination CassandraSQLContext provider
    bind(classOf[CassandraSQLContext]).annotatedWith(Names.named("DataStoreCassandraSQLContext")).toProvider(classOf[CassandraSQLContextProvider])

    //-- Bind destination SparkCassandraRepository
    bind(classOf[SparkDataStoreRepository]).annotatedWith(Names.named("DataStoreCassandraRepository")).to(classOf[SparkCassandraRepository])

    //right()
  }
}
