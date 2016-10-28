package com.intuit.wasabi.data.app

import cats.data.Xor
import cats.data.Xor._
import com.google.inject.Inject
import com.intuit.wasabi.data.exception.WasabiError
import com.intuit.wasabi.data.processor.SparkProcessor
import com.intuit.wasabi.data.processor.migratedata.CopyTableMigrationProcessor
import com.typesafe.config.Config
import org.apache.commons.lang3.ArrayUtils
import org.apache.spark.SparkContext

/**
  * This application is created to migrate data from source DataStore to destination DataStore.
  * It follows plug-able design and so executes the processors listed in the config "migrate-data.migration.processors"
  *
  *
  */
class MigrateDataApplication(sc: SparkContext, appConfig: Config) extends BaseSparkApplication (sc, appConfig)   {

 /**
    * Read processors listed in the config "migrate-data.migration.processors" and then execute them one-by-one.
    *
    */
  var copyTableMigrationProcessor: CopyTableMigrationProcessor = null

  @Inject
  def this(sc: SparkContext, appConfig: Config, iCopyTableMigrationProcessor: CopyTableMigrationProcessor, mType: String = null) {
    this(sc, appConfig)
    this.copyTableMigrationProcessor=iCopyTableMigrationProcessor
  }

  override def run(): WasabiError Xor Unit = {
    if(log.isInfoEnabled()) log.info("run() - STARTED")
    val sTime = System.currentTimeMillis

    val mConfig = appConfig.getConfig("migration")
    val processorIds = mConfig.getString("processors").split("-")
    if(log.isDebugEnabled()) log.debug(s"processorIds => ${ArrayUtils.toString(processorIds)}")

    val copyTableMigrationProcessorClassName = classOf[CopyTableMigrationProcessor].getName
    if(log.isDebugEnabled()) log.debug(s"copyTableMigrationProcessorClassName => $copyTableMigrationProcessorClassName")

    for(processorId <- processorIds) {
      if(log.isDebugEnabled()) log.debug(s"processorId => $processorId")

      val pClass = mConfig.getString(s"${processorId}.class").trim
      if(log.isDebugEnabled()) log.debug(s"pClass => $pClass")

      var processor: SparkProcessor = null

      //@TODO add scala reflection code instead of CASEs
      pClass match {
        case copyTableMigrationProcessorClassName => {
          processor = copyTableMigrationProcessor
        }
        //case whoa  => log.error(s"Unexpected case: ${whoa.toString}")
      }

      processor.process(processorId) match {
        case Left(error: WasabiError) => return Left(error)
        case Right(unit: Unit) => {
          if(log.isDebugEnabled) log.debug(s"process('$processorId') finished successfully...")
        }
      }
    }

    if(perfLog.isInfoEnabled) perfLog.info(s"Time taken by MigrateDataApplication[$appId].start() = ${System.currentTimeMillis-sTime} ms")
    if(log.isInfoEnabled()) log.info("run() - FINISHED")

    right()
  }
}






