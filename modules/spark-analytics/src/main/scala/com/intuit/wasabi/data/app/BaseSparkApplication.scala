package com.intuit.wasabi.data.app

import cats.data.Xor
import cats.data.Xor._
import com.intuit.wasabi.data.exception.WasabiError
import com.intuit.wasabi.data.util.Utility
import com.intuit.wasabi.data.util.Constants._
import com.typesafe.config.Config
import org.apache.spark.{Logging, SparkContext}
import org.slf4j.LoggerFactory


/**
  * This is a abstract base class for all the spark applications & extends AppConfig trait for configuration access.
  * This class provides common implementation for init() and stop() methods.
  */
abstract class BaseSparkApplication (iSparkContext: SparkContext, appConfig: Config) extends Logging {
  val sc: SparkContext = iSparkContext
  var appId: String = null
  val util = Utility
  val perfLog = LoggerFactory.getLogger(PERFORMANCE_LOGGER)

  /**
    * This method is responsible for
    * 1.	Could be used to keep track/history of each executions of spark applications e.g. logging application-id,
    *     start-time,end-time,batch_id (identifier for the data processed),completion_status.
    *     This is mostly useful in the ETL based and cron based innovations.
    * 2.	To facilitate retry or repeated invocations of spark applications whenever applicable.
    */
  def init(): WasabiError Xor Unit = {
    if(log.isInfoEnabled) log.info("init() - STARTED")
    val sTime = System.currentTimeMillis

    appId = appConfig.getString(APP_ARG_APP_ID)
    if(log.isDebugEnabled) log.debug("appId => "+appId)

    //appConfig = getConfigForApp(appId)
    if(log.isDebugEnabled) log.debug("migrateDataAppConfig => "+appConfig.toString())

    if(perfLog.isInfoEnabled) perfLog.info(s"Time taken by BaseSparkApplication[$appId].init() = ${System.currentTimeMillis-sTime} ms")
    if(log.isInfoEnabled) log.info("init() - FINISHED")

    right()
  }

  /**
    * This is abstract method and each concrete SparkApplication is supposed to provide implementation of this method.
    */
  def run(): WasabiError Xor Unit

  /**
    * This method is responsible for
    * 1.  Stop sparkContext created
    * 2.  Capture performance stats etc
    */
  def stop(): WasabiError Xor Unit = {
    if(log.isInfoEnabled) log.info("stop() - STARTED")
    val sTime = System.currentTimeMillis

    if(sc!=null) {
      sc.stop()
    }

    if(perfLog.isInfoEnabled) perfLog.info(s"Time taken by BaseSparkApplication[$appId].stop() = ${System.currentTimeMillis-sTime} ms")
    if(log.isInfoEnabled) log.info("stop() - FINISHED")

    right()
  }

}
