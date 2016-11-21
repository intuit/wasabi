package com.intuit.wasabi.data.app

import java.sql.Date
import java.util.UUID

import cats.data.Xor
import cats.data.Xor._
import com.google.inject.Inject
import com.google.inject.name.Named
import com.intuit.wasabi.data.exception.WasabiError
import com.intuit.wasabi.data.repository.SparkDataStoreRepository
import com.intuit.wasabi.data.util.Constants
import com.typesafe.config.Config
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.storage.StorageLevel

/**
  * This application is created to execute adhoc SQL statements on DataStore(s). Mainly useful for debugging purpose.
  *
  *
  */
class ExecuteSqlApplication(sc: SparkContext, appConfig: Config) extends BaseSparkApplication (sc, appConfig)   {

  var repository: SparkDataStoreRepository = null

  @Inject
  def this(sc: SparkContext, appConfig: Config, @Named("DataStoreCassandraRepository") repository: SparkDataStoreRepository, mType: String = null) {
    this(sc, appConfig)
    this.repository=repository
  }

  override def run(): WasabiError Xor Unit = {
    if(log.isInfoEnabled()) log.info("run() - STARTED")
    val sTime = System.currentTimeMillis

    val sqlStatements = appConfig.getString("sql")
    if(log.isInfoEnabled()) log.info(s"sqlStatements => $sqlStatements")

    val limit = appConfig.getInt("limit")
    if(log.isInfoEnabled()) log.info(s"limit => $limit")

    val repartition = appConfig.getInt("repartition")
    if(log.isInfoEnabled()) log.info(s"repartition => $repartition")

    val storageLevel = appConfig.getString("StorageLevel")
    if(log.isInfoEnabled()) log.info(s"StorageLevel => $StorageLevel")

    val save = appConfig.getBoolean("save")
    if(log.isInfoEnabled()) log.info(s"save => $save")

    var sqlContext1: SQLContext = null
    repository.getSqlContext() match {
      case Left(error: WasabiError) => return Left(error)
      case Right(sqlContext2: SQLContext) => sqlContext1=sqlContext2
    }
    val sqlContext = sqlContext1
    import sqlContext.implicits._

    //sqlContext.udf.register("string_to_uuid", new StringToUUIDFunc)
    //sqlContext.udf.register("uuid_to_string", new UUIDToStringFunc)


    if(sqlStatements!=null && !sqlStatements.isEmpty) {
      var tempDF:DataFrame = null
      val sqlStmtArray:Array[String] = sqlStatements.split(";")
      for(i <- 0 to sqlStmtArray.length-1) {
        if(log.isInfoEnabled()) log.info(s"i > ${i}")

        val sqlStmt = sqlStmtArray(i)

        repository.read(sqlStmt.trim, false) match {
          case Left(error: WasabiError) => return left(error)
          case Right(df: DataFrame) => tempDF = df
        }

        if(i==(sqlStmtArray.length-1)) {
          if(repartition!=0) {
            tempDF = tempDF.repartition(repartition)
          }
          if(save) {
            tempDF = tempDF.cache()
            tempDF.show(limit)

            val resultFileName=s"${Constants.SQL_RESULT_OUTPUT_DIR}/sql_output_${System.currentTimeMillis}.csv"
            tempDF.write.format(Constants.SPARK_SQL_CSV_FORMAT).option("header", "true").save(resultFileName);
            if(log.isInfoEnabled()) log.info(s"SQL output is stored @ ${resultFileName}")
          } else {
            tempDF.show(limit)
          }
        } else {
          tempDF.registerTempTable(s"TempTbl_${i+1}")
        }
      }
    }

    if(perfLog.isInfoEnabled) perfLog.info(s"Time taken by ExecuteSqlApplication[$appId].start() = ${System.currentTimeMillis-sTime} ms")
    if(log.isInfoEnabled()) log.info("run() - FINISHED")

    right()
  }
}





