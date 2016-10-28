package com.intuit.wasabi.data.conf.guice.migratedata

import cats.data.Xor._
import com.datastax.spark.connector.cql.CassandraConnector
import com.google.inject.name.Names
import com.intuit.wasabi.data.conf.guice.CommonSparkApplicationDI
import com.intuit.wasabi.data.repository.{DataStoreConnectionProperties, SparkDataStoreRepository}
import com.intuit.wasabi.data.util.Utility
import com.intuit.wasabi.data.util.Constants._
import com.typesafe.config.Config
import org.apache.spark.sql.cassandra.CassandraSQLContext



class MigrateDataApplicationDI(appConfig: Config) extends CommonSparkApplicationDI(appConfig) {

  override def configure(): Unit = {
    super.configure()

    val mConfig = Utility.configToMap(appConfig.getConfig("migration"))

    //---------------------------------------------------------------
    //------- Cassandra To Cassandra DataStore Repositories ---------
    //---------------------------------------------------------------

    //------------ Source DataStore ------------
    //Read DataStore connection configs - source
    val sDataStoreType = mConfig.get("DataStores.src.datastoreType").get
    val sCluster = mConfig.get("DataStores.src.cluster").get
    val sHost = mConfig.get("DataStores.src.host").get
    val sPort = mConfig.get("DataStores.src.port").get
    val sKeyspace = mConfig.get("DataStores.src.keyspace").get

    sDataStoreType match {
      case DATASTORE_CASSANDRA => {
        //Create & bind source DataStoreConnectionProperties
        bind(classOf[DataStoreConnectionProperties]).annotatedWith(Names.named("SourceDataStoreConnectionProperties")).toInstance(new DataStoreConnectionProperties(Map(KEY_SPARK_CASSANDRA_CONN_CLUSTER -> sCluster, KEY_SPARK_CASSANDRA_CONN_HOST -> sHost, KEY_SPARK_CASSANDRA_CONN_PORT -> sPort, KEY_SPARK_CASSANDRA_CONN_KEYSPACE -> sKeyspace)))

        //-- Bind source CassandraConnector provider
        bind(classOf[CassandraConnector]).annotatedWith(Names.named("SourceDataStoreCassandraConnector")).toProvider(classOf[DestinationCassandraConnectorProvider])

        //-- Bind destination CassandraSQLContext provider
        bind(classOf[CassandraSQLContext]).annotatedWith(Names.named("SourceDataStoreCassandraSQLContext")).toProvider(classOf[CassandraSQLContextProvider])

        //-- Bind source SparkCassandraRepository
        bind(classOf[SparkDataStoreRepository]).annotatedWith(Names.named("SourceDataStoreRepository")).to(classOf[SourceSparkCassandraRepository])
      }
      case not_supported => {
        val msg = s"Source DataStore type is not yet supported = $sDataStoreType"
        log.error(msg)
      }
    }

    //------------ Destination DataStore ------------
    //Read DataStore connection configs - destination
    val dDataStoreType = mConfig.get("DataStores.dest.datastoreType").get
    val dCluster = mConfig.get("DataStores.dest.cluster").get
    val dHost = mConfig.get("DataStores.dest.host").get
    val dPort = mConfig.get("DataStores.dest.port").get
    val dKeyspace = mConfig.get("DataStores.dest.keyspace").get

    dDataStoreType match {
      case DATASTORE_CASSANDRA => {
        //Create & bind destination DataStoreConnectionProperties
        bind(classOf[DataStoreConnectionProperties]).annotatedWith(Names.named("DestinationDataStoreConnectionProperties")).toInstance(new DataStoreConnectionProperties(Map(KEY_SPARK_CASSANDRA_CONN_CLUSTER -> dCluster, KEY_SPARK_CASSANDRA_CONN_HOST -> dHost, KEY_SPARK_CASSANDRA_CONN_PORT -> dPort, KEY_SPARK_CASSANDRA_CONN_KEYSPACE -> dKeyspace)))

        //-- Bind destination CassandraConnector provider
        bind(classOf[CassandraConnector]).annotatedWith(Names.named("DestinationDataStoreCassandraConnector")).toProvider(classOf[DestinationCassandraConnectorProvider])

        //-- Bind destination CassandraSQLContext provider
        bind(classOf[CassandraSQLContext]).annotatedWith(Names.named("DestinationDataStoreCassandraSQLContext")).toProvider(classOf[CassandraSQLContextProvider])

        //-- Bind destination SparkCassandraRepository
        bind(classOf[SparkDataStoreRepository]).annotatedWith(Names.named("DestinationDataStoreRepository")).to(classOf[DestinationSparkCassandraRepository])
      }
      case not_supported => {
        val msg = s"Destination DataStore type is not yet supported = $dDataStoreType"
        log.error(msg)
      }
    }

    //right()
  }
}

//------ Provider for SparkContext

