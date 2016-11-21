package com.intuit.wasabi.tests.data.conf.guice.migratedata

import com.datastax.spark.connector.cql.CassandraConnector
import com.google.inject.name.{Named, Names}
import com.google.inject.{Inject, Provider}
import com.intuit.wasabi.data.conf.guice.CommonSparkApplicationDI
import com.intuit.wasabi.data.conf.guice.migratedata.SourceSparkCassandraRepository
import com.intuit.wasabi.data.repository.cassandra.SparkCassandraRepository
import com.intuit.wasabi.data.repository.{DataStoreConnectionProperties, SparkDataStoreRepository}
import com.intuit.wasabi.data.udf.CurrentTimestampFunc
import com.intuit.wasabi.data.util.Constants._
import com.typesafe.config.Config
import org.apache.spark.SparkContext
import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.{DataFrame, DataFrameWriter, SQLContext, SaveMode}

import scala.collection.mutable

/**
  * Created by nbarge on 10/19/16.
  */

class MockMigrateDataApplicationDI(appConfig: Config, sc: SparkContext) extends CommonSparkApplicationDI(appConfig, Option.apply(sc)) {

  override def configure(): Unit = {
    super.configure()

    val mConfig = Utility.configToMap(appConfig.getConfig("migration"))

    //---------------------------------------------------------------
    //------- Cassandra To Cassandra DataStore Repositories ---------
    //---------------------------------------------------------------

    //------------ Source DataStore ------------
    //Read DataStore connection configs - source
    val sDataStoreType = mConfig.get("datastores.src.type").get
    val sCluster = mConfig.get("datastores.src.cluster").get
    val sHost = mConfig.get("datastores.src.host").get
    val sPort = mConfig.get("datastores.src.port").get
    val sKeyspace = mConfig.get("datastores.src.keyspace").get

    sDataStoreType match {
      case DATASTORE_CASSANDRA => {
        //Create & bind source DataStoreConnectionProperties
        bind(classOf[DataStoreConnectionProperties]).annotatedWith(Names.named("SourceDataStoreConnectionProperties")).toInstance(new DataStoreConnectionProperties(Map(KEY_SPARK_CASSANDRA_CONN_CLUSTER -> sCluster, KEY_SPARK_CASSANDRA_CONN_HOST -> sHost, KEY_SPARK_CASSANDRA_CONN_PORT -> sPort, KEY_SPARK_CASSANDRA_CONN_KEYSPACE -> sKeyspace)))

        //-- Bind source CassandraConnector provider
        bind(classOf[CassandraConnector]).annotatedWith(Names.named("SourceDataStoreCassandraConnector")).toProvider(classOf[DestinationCassandraConnectorProvider])

        //-- Bind destination CassandraSQLContext provider
        bind(classOf[CassandraSQLContext]).annotatedWith(Names.named("SourceDataStoreCassandraSQLContext")).toProvider(classOf[MockCassandraSQLContextProvider])

        //-- Bind source SparkCassandraRepository
        bind(classOf[SparkDataStoreRepository]).annotatedWith(Names.named("SourceDataStoreRepository")).to(classOf[SourceSparkCassandraRepository])
      }
      case not_supported => {
        val msg = s"Source DataStore type is not yet supported = $sDataStoreType"
        log.error(msg)
        //return left(WasabiError.ConfigurationError(InvalidConfigurationError(msg)))
      }
    }

    //------------ Destination DataStore ------------
    //Read DataStore connection configs - destination
    val dDataStoreType = mConfig.get("datastores.dest.type").get
    val dCluster = mConfig.get("datastores.dest.cluster").get
    val dHost = mConfig.get("datastores.dest.host").get
    val dPort = mConfig.get("datastores.dest.port").get
    val dKeyspace = mConfig.get("datastores.dest.keyspace").get

    dDataStoreType match {
      case DATASTORE_CASSANDRA => {
        //Create & bind destination DataStoreConnectionProperties
        bind(classOf[DataStoreConnectionProperties]).annotatedWith(Names.named("DestinationDataStoreConnectionProperties")).toInstance(new DataStoreConnectionProperties(Map(KEY_SPARK_CASSANDRA_CONN_CLUSTER -> dCluster, KEY_SPARK_CASSANDRA_CONN_HOST -> dHost, KEY_SPARK_CASSANDRA_CONN_PORT -> dPort, KEY_SPARK_CASSANDRA_CONN_KEYSPACE -> dKeyspace)))

        //-- Bind destination CassandraConnector provider
        bind(classOf[CassandraConnector]).annotatedWith(Names.named("DestinationDataStoreCassandraConnector")).toProvider(classOf[DestinationCassandraConnectorProvider])

        //-- Bind destination CassandraSQLContext provider
        bind(classOf[CassandraSQLContext]).annotatedWith(Names.named("DestinationDataStoreCassandraSQLContext")).toProvider(classOf[MockCassandraSQLContextProvider])

        //-- Bind destination SparkCassandraRepository
        bind(classOf[SparkDataStoreRepository]).annotatedWith(Names.named("DestinationDataStoreRepository")).to(classOf[MockDestinationSparkCassandraRepository])
      }
      case not_supported => {
        val msg = s"Destination DataStore type is not yet supported = $dDataStoreType"
        log.error(msg)
        //return left(WasabiError.ConfigurationError(InvalidConfigurationError(msg)))
      }
    }

    //right()
  }
}

//------ Provider for SQLContext
class MockCassandraSQLContextProvider extends Provider[CassandraSQLContext] {
  var sc: SparkContext = null

  @Inject
  def this(sc: SparkContext) {
    this
    this.sc=sc
  }

  override def get(): CassandraSQLContext = {
    new MockCassandraSQLContext(sc)
  }
}

class MockCassandraSQLContext(sc: SparkContext) extends CassandraSQLContext(sc) {
  val reqResMap:mutable.Map[String,DataFrame] = mutable.Map()

  override def sql(cassandraQuery: String): DataFrame = {
    reqResMap(cassandraQuery.trim.toLowerCase)
  }

  def mock(cassandraQuery: String, res: DataFrame): MockCassandraSQLContext = {
    reqResMap.put(cassandraQuery.trim.toLowerCase, new MockDataFrame(res.sqlContext, res.queryExecution.logical))
    this
  }
}

class MockDataFrame(sqlContext: SQLContext, logicalPlan: LogicalPlan) extends DataFrame(sqlContext, logicalPlan) {
  var df: DataFrame = null
  def this(df: DataFrame) {
    this(df.sqlContext, df.queryExecution.logical)
    this.df=df
  }

  override def write(): DataFrameWriter = {
    df.write
  }
}

//------ Provider for destination CassandraRepository
class MockDestinationSparkCassandraRepository(sc: SparkContext, options: DataStoreConnectionProperties, connection: CassandraConnector, sqlContext: CassandraSQLContext, currentTimestampFunc: CurrentTimestampFunc) extends SparkCassandraRepository(sc, options, connection, sqlContext, currentTimestampFunc) {
  @Inject
  def this(sc: SparkContext, @Named("DestinationDataStoreConnectionProperties") options: DataStoreConnectionProperties, @Named("DestinationDataStoreCassandraConnector") connection: CassandraConnector, @Named("DestinationDataStoreCassandraSQLContext") sqlContext: CassandraSQLContext, currentTimestampFunc: CurrentTimestampFunc, rType: String = null ) {
    this(sc, options, connection, sqlContext, currentTimestampFunc)
  }

  /** This method is created facilitate mocking of write() operation
    *
    * @param df
    * @param saveMode
    * @param saveOptions
    */
  override def writeDataFrame(df: DataFrame, saveMode: SaveMode, saveOptions: Map[String, String]): Unit = {
    log.info("Doing nothing ---------- it is a mock operation.....")
  }
}

