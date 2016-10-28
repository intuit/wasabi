package com.intuit.wasabi.data.launcher

import org.apache.commons.lang.ArrayUtils
import cats.data.Xor
import cats.data.Xor._
import com.google.inject.Guice
import com.intuit.wasabi.data.util.Constants._
import com.intuit.wasabi.data.app.{BaseSparkApplication, MigrateDataApplication}
import com.intuit.wasabi.data.conf.AppConfig
import com.intuit.wasabi.data.conf.guice.migratedata.MigrateDataApplicationDI
import com.intuit.wasabi.data.exception.{ApplicationException, WasabiError}

/**
  * This is the entry point for all the spark applications. This class looks for app_id argument and then
  * create an instance of concrete spark application e.g. MigrationDataApplication and then calls init(), start()
  * and stop() methods in sequence.
  *
  */
object SparkApplicationLauncher extends AppConfig {

  def main(args: Array[String]): Unit = {
    val sTime=System.currentTimeMillis
    if(log.isInfoEnabled) log.info(s"STARTED - args=${ArrayUtils.toString(args)}")

    val appId = rootConfig.getString("app_id")
    if(appId==null) {
      throw new ApplicationException("Please provide application id as VM option: -Dapp_id=<app_id> ")
    }

    var app: BaseSparkApplication = null
    val appConfig = getConfigForApp(appId)
    if(log.isDebugEnabled) log.debug(s"appConfig.appId => ${appConfig.getString("app_id")}" )

    appId match {
      case APP_ID_MIGRATE_DATA => {
        val injector = Guice.createInjector(new MigrateDataApplicationDI(appConfig))
        app = injector.getInstance(classOf[MigrateDataApplication])
      }
      //case APP_ID_DAILY_AGGREGATION => {}
      //case APP_ID_REAL_TIME => {}
      case not_matching =>   {
        log.error("Couldn't find spark application for given app_id...")
        System.exit(1)
      }
    }

    var isSuccess = true

    try {
      app.init() match {
        case Left(error: WasabiError) => {
          log.error(s"Error occurred while initializing SparkApplication.. \n$error")
          isSuccess=false
        }
        case Right(unit: Unit) => {
          if (log.isDebugEnabled) log.debug("init() finished successfully...")
        }
      }

      if (isSuccess) {
        app.run() match {
          case Left(error: WasabiError) => {
            log.error(s"Error occurred while running SparkApplication.. \n$error")
            isSuccess=false
          }
          case Right(unit: Unit) => {
            if (log.isDebugEnabled) log.debug("run() finished successfully...")
          }
        }
      }
    } catch {
      case ex: Throwable => {
        isSuccess=false
        log.error(s"Underline librabry exception occurred..", ex)
      }
    } finally {
      try {
        app.stop() match {
          case Left(error: WasabiError) => {
            log.error(s"Error occurred while stopping SparkApplication.. \n$error")
            isSuccess=false
          }
          case Right(unit: Unit) => {
            if(log.isDebugEnabled) log.debug("stop() finished successfully...")
          }
        }
      } catch {
        case ex: Throwable => {
          isSuccess=false
          log.error(s"Underline librabry exception occurred while stopping spark context..", ex)
        }
      }
    }

    if(perfLog.isInfoEnabled()) perfLog.info(s"Time taken by SparkApplication[$appId] CompletionStatus[$isSuccess] = ${System.currentTimeMillis-sTime} ms")
    if(log.isInfoEnabled) log.info(s"SparkApplication[$appId] CompletionStatus[$isSuccess] - FINISHED")

    if(isSuccess) {
      System.exit(0)
    } else {
      System.exit(1)
    }
  }
}
