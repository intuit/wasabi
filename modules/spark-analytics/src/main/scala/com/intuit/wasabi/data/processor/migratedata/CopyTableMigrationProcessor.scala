package com.intuit.wasabi.data.processor.migratedata

import cats.data.Xor
import cats.data.Xor._
import com.google.inject.Inject
import com.google.inject.name.Named
import com.intuit.wasabi.data.exception.WasabiError
import com.intuit.wasabi.data.processor.SparkProcessor
import com.intuit.wasabi.data.repository.SparkDataStoreRepository
import com.typesafe.config.Config
import org.apache.spark.SparkContext
import org.apache.spark.sql.{DataFrame, SQLContext, SaveMode}

/**
  * This processor is created to copy data from source table to destination.
  * Source and destination tables details are driven by configurations migrate-data.migration.*
  *
  */

class CopyTableMigrationProcessor extends SparkProcessor {
  var sc: SparkContext =null
  var appConfig: Config = null
  var sRepository: SparkDataStoreRepository = null
  var dRepository: SparkDataStoreRepository = null

  /*  def this(processorId: String, sc: SparkContext, argMap:Map[String,String], appConfig: Config) {
      this()
      if(log.isDebugEnabled) log.debug("this() - STARTED")

      this.ID=processorId
      this.sc=sc
      this.argMap=argMap
      this.appConfig=appConfig

      if(log.isDebugEnabled) log.debug("this() - FINISHED")
    }*/

  /**
    * This method reads configuration to find out source and destination DataStore details and
    * then reads data from source DataStore/table and writes to destination DataStore/table.
    */

  @Inject
  def this(sc: SparkContext, appConfig: Config, @Named("SourceDataStoreRepository") iSourceDataStoreRepository: SparkDataStoreRepository, @Named("DestinationDataStoreRepository") iDestinationDataStoreRepository: SparkDataStoreRepository) {
    this
    if(log.isDebugEnabled) log.debug("this() - STARTED")

    this.sc=sc
    this.appConfig=appConfig
    this.sRepository=iSourceDataStoreRepository
    this.dRepository=iDestinationDataStoreRepository

    if(log.isDebugEnabled) log.debug("this() - FINISHED")
  }

  override def process(processorID: String) : WasabiError Xor Unit = {
    if(log.isInfoEnabled) log.info(s"Processor[$processorID] - STARTED")
    val sTime = System.currentTimeMillis

    val mConfig = util.configToMap(appConfig.getConfig("migration"))
    if(log.isDebugEnabled) log.debug("migration configs => "+mConfig.toString())

    //Read source table name to copy data from
    //Read destination table name to copy data to
    //Read select projection. Default is *, means read all columns without any change.
    //Read WHERE clause. Default is empty, means no filtration.
    //Read SaveMode implicitly applied for destination table. Default is 'Append' mode, means add new and replace existing data
    val srcTable = mConfig.get(processorID+".src.table").get
    val destTable = mConfig.get(processorID+".dest.table").get
    val selectProjection = mConfig.getOrElse(processorID+".src.select_projection", "*")
    var whereClause = mConfig.getOrElse(processorID+".src.where_clause", "")
    val saveMode = mConfig.getOrElse(processorID+".dest.saveMode", "Append")
    if(log.isDebugEnabled) log.debug(s"srcTable=$srcTable, destTable=$destTable, selectProjection=$selectProjection, whereClause=$whereClause, saveMode=$saveMode")

    //Reading data from source DataStore
    var sSelectSQL=s"SELECT $selectProjection FROM $srcTable"
    //Apply filters from configurations if defined
    if(whereClause!=null && !whereClause.isEmpty) {
      val currentTS = System.currentTimeMillis()
      whereClause = whereClause.replaceAll("CTMP_CURRENT_TIMESTAMP", currentTS.toString)
      sSelectSQL += s" WHERE $whereClause"
    }
    var srcDF: DataFrame = null
    if(log.isInfoEnabled) log.info(s"Reading DataFrame from '$srcTable' table... Using SQL='$sSelectSQL'")
    sRepository.read(sSelectSQL) match {
      case Left(error: WasabiError) => return left(error)
      case Right(df: DataFrame) => srcDF=df
    }

    if(log.isDebugEnabled) {
      log.debug(s"Source table [$srcTable] 10 rows: ")
      srcDF.show(10)
      srcDF.printSchema()
    }

    //Copy source data as is in to the destination DataStore
    val destDF=srcDF

    //Write to destination DataStore
    if(log.isDebugEnabled) log.debug(s"Writing dest DataFrame to $destTable table... ")
    dRepository.write(destTable, SaveMode.valueOf(saveMode), destDF)

    if(perfLog.isInfoEnabled()) perfLog.info(s"Time taken by Processor[$processorID] = ${System.currentTimeMillis-sTime} ms")
    if(log.isInfoEnabled) log.info(s"Processor[$processorID] - FINISHED - Copy from $srcTable to $destTable DONE...\n\n")

    right()
  }
}


