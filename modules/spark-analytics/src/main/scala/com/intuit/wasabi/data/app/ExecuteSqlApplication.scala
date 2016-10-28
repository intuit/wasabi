package com.intuit.wasabi.data.app

import cats.data.Xor
import cats.data.Xor._
import com.google.inject.Inject
import com.intuit.wasabi.data.exception.WasabiError
import com.intuit.wasabi.data.repository.SparkDataStoreRepository
import com.typesafe.config.Config
import org.apache.commons.lang3.ArrayUtils
import org.apache.spark.SparkContext
import org.apache.spark.sql.DataFrame

/**
  * This application is created to execute adhoc SQL statements on DataStore(s). Mainly useful for debugging purpose.
  *
  *
  */
class ExecuteSqlApplication(sc: SparkContext, appConfig: Config) extends BaseSparkApplication (sc, appConfig)   {

  var repository: SparkDataStoreRepository = null

  @Inject
  def this(sc: SparkContext, appConfig: Config, repository: SparkDataStoreRepository, mType: String = null) {
    this(sc, appConfig)
    this.repository=repository
  }

  override def run(): WasabiError Xor Unit = {
    if(log.isInfoEnabled()) log.info("start() - STARTED")
    val sTime = System.currentTimeMillis

    val sqlStatements = appConfig.getString("sql")
    if(log.isDebugEnabled()) log.debug(s"sqlStatements => $sqlStatements")

    val limit = appConfig.getString("limit")
    if(log.isDebugEnabled()) log.debug(s"limit => $limit")

    if(sqlStatements!=null && !sqlStatements.isEmpty) {
      val sqlStmtArray:Array[String] = sqlStatements.split(";")
      for(sqlStmt <- sqlStmtArray) {
        var resultDF: DataFrame = null
        repository.read(sqlStmt) match {
          case Left(error: WasabiError) => return left(error)
          case Right(df: DataFrame) => resultDF=df
        }

      }
    }

    if(perfLog.isInfoEnabled) perfLog.info(s"Time taken by ExecuteSqlApplication[$appId].start() = ${System.currentTimeMillis-sTime} ms")
    if(log.isInfoEnabled()) log.info("start() - FINISHED")

    right()
  }
}






