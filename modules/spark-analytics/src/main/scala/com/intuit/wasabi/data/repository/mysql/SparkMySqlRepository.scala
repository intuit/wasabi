package com.intuit.wasabi.data.repository.mysql

import cats.data.Xor
import cats.data.Xor._
import com.intuit.wasabi.data.exception.{RepositoryException, WasabiError}
import com.intuit.wasabi.data.repository.SparkDataStoreRepository
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode}

final case class SparkMySqlRepositoryNotYetSupported() extends RepositoryException("SparkMySqlRepository is not yet supported..")
/**
  * This will be a concrete repository to interact with the MySQL DataStore.
  * Yet Not implemented - not planned to implement as a part of data model migration efforts.
  *
  * */
class SparkMySqlRepository extends SparkDataStoreRepository  {

  def this(sc: SparkContext, options: Map[String, String]) {
    this()
  }

  override def read(sql: String): WasabiError Xor DataFrame = {
    left(WasabiError.RepositoryError(SparkMySqlRepositoryNotYetSupported()))
  }

  override def write(tableName: String, saveMode: SaveMode, data: DataFrame): WasabiError Xor Unit = {
    left(WasabiError.RepositoryError(SparkMySqlRepositoryNotYetSupported()))
  }

  override def execDDL(statements: String): WasabiError Xor Unit = {
    left(WasabiError.RepositoryError(SparkMySqlRepositoryNotYetSupported()))
  }

  override def getSqlContext(): WasabiError Xor SQLContext = {
    left(WasabiError.RepositoryError(SparkMySqlRepositoryNotYetSupported()))
  }
}