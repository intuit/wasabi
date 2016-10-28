package com.intuit.wasabi.data.repository

import cats.data.Xor
import com.intuit.wasabi.data.conf.AppConfig
import com.intuit.wasabi.data.exception.WasabiError
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode}

/**
  * Interface to interact with the DataStores by spark applications/processors.
  *
  */
trait SparkDataStoreRepository extends AppConfig {
  /**
    * Execute spark-sql query directly on DataStore table.
    *
    * @param sql spark-sql compatible query
    * @return result of sql query in the form of DataFrame
    *
    */
  def read(sql:String) : WasabiError Xor DataFrame

  /**
    * Write/Insert given data in a given mode to given table.
    *
    * @param tableName where given data (DataFrame) needs to be written.
    * @param saveMode specifies how data should be written e.g. Append, Overwrite etc.
    * @param data actual data in the form of DataFrame that needs to be written to the given table.
    */
  def write(tableName:String, saveMode: SaveMode, data: DataFrame) : WasabiError Xor Unit


  /**
    * This method provides a way to execute DDL statements on DataStore.
    *
    * @param statements DDL statement that needs to be executed.
    *
    */
  def execDDL(statements: String) : WasabiError Xor Unit

  /**
    * This method provides access to the SQLContext so that application/processor layer can make use of
    * different spark-sql functionality like creating a temp tables, caching it etc
    *
    * @return the SQLContext
    *
    */
  def getSqlContext() : WasabiError Xor SQLContext
}
