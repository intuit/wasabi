package com.intuit.wasabi.data.repository.cassandra

import java.sql.Timestamp
import java.time._

import cats.data.Xor
import cats.data.Xor._
import com.datastax.spark.connector.cql.CassandraConnector
import com.google.inject.Inject
import com.google.inject.name.Named
import com.intuit.wasabi.data.exception.{RepositoryException, UnderlineRepositoryException, WasabiError}
import com.intuit.wasabi.data.exception.WasabiError.RepositoryError
import com.intuit.wasabi.data.launcher.SparkApplicationLauncher._
import com.intuit.wasabi.data.repository.{DataStoreConnectionProperties, SparkDataStoreRepository}
import com.intuit.wasabi.data.udf.CurrentTimestampFunc
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode}
import com.intuit.wasabi.data.util.Constants._

/*
  * Errors:
  */
final case class RequiredOptionsMap() extends RepositoryException("'options' map (containing host,port,keyspace,cluster) is mandatory to create SparkCassandraRepository..")
final case class RequiredKeyspaceOption() extends RepositoryException("'keyspace' is mandatory to create SparkCassandraRepository... Please pass it as a part of 'options'")

final case class RequiredSqlQuery() extends RepositoryException("SQL query is mandatory to for the read() operation..")

final case class CassandraConnectionNotInitialized() extends RepositoryException("Cassandra connection is not initialized, can not execute execDLL() operation..")
final case class RequiredStatements() extends RepositoryException("DDL statements are mandatory to execute execDLL() operation..")


/**
  * This is a concrete repository to interact with the Cassandra DataStore. As a constructor, it takes sparkContext
  * and options Map containing clusterName, host, port & keyspace to connect to.
  *
  * This class makes use of CassandraSQLContext to read data from the cassandra.
  * This class makes use of DataFrame.write() operation to write data to cassandra.
  */

class SparkCassandraRepository extends SparkDataStoreRepository {
  var sqlContext: CassandraSQLContext = null
  var connection: CassandraConnector = null
  var keySpaceName: String = null
  var clusterName: String = null

  /**
    * Constructor is used to create CassandraSQLContext and CassandraConnector to execute DDLs
    *
    * @param sc Spark Context
    * @param dbProperties to provide details of the DataStore with following as keys
    *                [spark.cassandra.connection.cluster, spark.cassandra.connection.host,
    *                spark.cassandra.connection.port,spark.cassandra.connection.keyspace]
    */

  def this(sc: SparkContext, dbProperties: DataStoreConnectionProperties, connection: CassandraConnector, sqlContext: CassandraSQLContext, currentTimestampFunc: CurrentTimestampFunc) {
    this()
    if(log.isInfoEnabled) log.info("this() - STARTED")
    val sTime = System.currentTimeMillis

    this.sqlContext=sqlContext
    if(dbProperties==null) {
      throw RequiredOptionsMap()
    }

    clusterName = dbProperties.vals.getOrElse(KEY_SPARK_CASSANDRA_CONN_CLUSTER, DEFAULT_CLUSTER_NAME)
    keySpaceName = dbProperties.vals.getOrElse(KEY_SPARK_CASSANDRA_CONN_KEYSPACE, null)

    if(keySpaceName==null) throw RequiredKeyspaceOption()

    //Not supported in spark1.4: sqlContext.setCluster(clusterName)
    this.sqlContext.setKeyspace(keySpaceName)

    this.sqlContext.udf.register("wasabi_current_timestamp", currentTimestampFunc)

    if(perfLog.isInfoEnabled()) perfLog.info(s"Time taken by SparkCassandraRepository.this = ${System.currentTimeMillis-sTime} ms")
    if(log.isInfoEnabled) log.info("this() - FINISHED")
  }

  /**
    * Execute spark-sql query directly on cassandra table.
    *
    * @param sql spark-sql compatible query
    * @return result of sql query in the form of DataFrame
    *
    */
  override def read(sql: String): WasabiError Xor DataFrame = {
    val sTime = System.currentTimeMillis
    if(log.isDebugEnabled) log.debug(s"read() - STARTED - sql=$sql")
    var result: DataFrame =null
    if (sql == null) {
      left(RequiredSqlQuery)
    }

    //This statement set current cassandra connection
    implicit val c = connection
    //Execute spark sql query
    result = sqlContext.sql(sql)
    if(log.isDebugEnabled) {
      log.debug("Read result: ")
      result.show(10)
    }

    if (perfLog.isInfoEnabled()) perfLog.info(s"Time taken by SparkCassandraRepository.read = ${System.currentTimeMillis - sTime} ms")
    if (log.isDebugEnabled) log.debug("read() - FINISHED")
    right(result)
  }

  /**
    * Write/Insert given data in a given mode to given table.
    *
    * @param tableName where given data (DataFrame) needs to be written.
    * @param saveMode specifies how data should be written e.g. Append, Overwrite etc.
    * @param data actual data in the form of DataFrame that needs to be written to the given table.
    */
  override def write(tableName: String, saveMode: SaveMode, data: DataFrame): WasabiError Xor Unit = {
    val sTime = System.currentTimeMillis
    if(log.isDebugEnabled) log.debug("write() - STARTED - tableName="+tableName+", saveMode="+saveMode)
    val saveOptions = Map(
      "table" -> tableName,
      "keyspace" -> keySpaceName,
      "cluster" -> clusterName)

    //This statement set current cassandra connection
    implicit val c = connection

    writeDataFrame(data, saveMode, saveOptions)

    if (perfLog.isInfoEnabled()) perfLog.info(s"Time taken by SparkCassandraRepository.write = ${System.currentTimeMillis - sTime} ms")
    if (log.isDebugEnabled) log.debug("write() - FINISHED")
    right()
  }

  /**
    * This method is created facilitate mocking of write() operation
    *
    * @param df
    * @param saveMode
    * @param saveOptions
    */
  def writeDataFrame(df: DataFrame, saveMode: SaveMode, saveOptions: Map[String,String]): Unit = {
    df.write.format(SPARK_SQL_CASSANDRA_FORMAT).mode(saveMode).options(saveOptions).save()
  }

  /**
    * This method provides a way to execute DDL statements on cassandra.
    *
    * @param statements DDL statement(s) that needs to be executed - separated by semicolons.
    *
    */
  override def execDDL(statements: String): WasabiError Xor Unit = {
    val sTime = System.currentTimeMillis
    if(log.isDebugEnabled) log.debug(s"execDDL() - STARTED - statements=$statements")
    if(connection==null) left(WasabiError.RepositoryError(CassandraConnectionNotInitialized()))
    if(statements==null) left(WasabiError.RepositoryError(RequiredStatements()))
    val session = connection.openSession()

    for(statement <- statements.split(";"))  {
      if (log.isDebugEnabled) log.debug(s"SparkCassandraRepository:execDDL - now executing - statement=$statement")
      session.execute(statement)
    }

    session.close()
    if (perfLog.isInfoEnabled()) perfLog.info(s"Time taken by SparkCassandraRepository.execDDL = ${System.currentTimeMillis - sTime} ms")
    if (log.isDebugEnabled) log.debug("execDDL() - FINISHED")

    right()
  }

  /**
    * This method provides access to the SQLContext so that application/processor layer can make use of
    * different spark-sql functionality like creating a temp tables, caching it etc
    *
    * @return the CassandraSQLContext as a SQLContext
    *
    */
  override def getSqlContext(): WasabiError Xor SQLContext  = {
    right(sqlContext)
  }
}
