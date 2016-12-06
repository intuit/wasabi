package com.intuit.wasabi.sparkanalytics.data.app

import java.sql.Timestamp

import cats.data.Xor
import cats.data.Xor._
import com.google.inject.Guice
import com.holdenkarau.spark.testing.SharedSparkContext
import com.intuit.wasabi.data.app.MigrateDataApplication
import com.intuit.wasabi.data.conf.AppConfig
import com.intuit.wasabi.data.conf.guice.migratedata.MigrateDataApplicationDI
import com.intuit.wasabi.data.exception.{ProcessorException, WasabiError}
import com.intuit.wasabi.data.processor.migratedata.CopyTableMigrationProcessor
import com.typesafe.config.ConfigFactory
import org.apache.spark.Logging
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
  * Created by nbarge on 10/21/16.
  */

@RunWith(classOf[JUnitRunner])
class MigrateDataApplicationTests extends FunSuite with SharedSparkContext with MockFactory with Logging {

  var setting: AppConfig = null

  override def beforeAll() {
    super.beforeAll()
    log.info("Setting app_id in the VM arguments...")
    println("******************** Setting app_id in the VM arguments *******************")

    val jMap: java.util.Map[String,String] = new java.util.HashMap[String,String]()
    jMap.put("app_id", "migrate-data")
    jMap.put("master","local[2]")

    setting = new AppConfig(Option.apply(ConfigFactory.parseMap(jMap)))
  }

  test("Processor works fine...") {
    val appConfig = setting.getConfigForApp("migrate-data")
    val injector = Guice.createInjector(new MigrateDataApplicationDI(appConfig, Option(sc)))
    val app = injector.getInstance(classOf[MigrateDataApplication])

    val processor: CopyTableMigrationProcessor = stub[CopyTableMigrationProcessor]

    //configure stubs
    val emptyRight:Xor[WasabiError, Unit] = Right()
    (processor.process _).when("CopyUserAssignmentTblProcessor").returns(emptyRight)

    val mApp = new MigrateDataApplication(sc, appConfig, processor)

    var isSuccess = false
    mApp.init() match {
      case Left(err: WasabiError) => isSuccess = false
      case Right(succ: Unit) => isSuccess = true
    }
    assert(isSuccess, "Couldn't initialize application...")

    mApp.run() match {
      case Left(err: WasabiError) => isSuccess = false
      case Right(succ: Unit) => isSuccess = true
    }
    assert(isSuccess, "run() should finish successfully if processor works fine...")
    //app.stop()
  }

  test("Processor returns error...") {
    val appConfig = setting.getConfigForApp("migrate-data")
    val injector = Guice.createInjector(new MigrateDataApplicationDI(appConfig, Option(sc)))
    val app = injector.getInstance(classOf[MigrateDataApplication])

    val processor: CopyTableMigrationProcessor = stub[CopyTableMigrationProcessor]

    //configure stubs
    val errorLeft:Xor[WasabiError, Unit] = Left(new WasabiError.ProcessorError(new ProcessorException("Some processor exception occurred...")))
    (processor.process _).when("CopyUserAssignmentTblProcessor").returns(errorLeft)

    val mApp = new MigrateDataApplication(sc, appConfig, processor)

    var isSuccess = false
    mApp.init() match {
      case Left(err: WasabiError) => isSuccess = false
      case Right(succ: Unit) => isSuccess = true
    }
    assert(isSuccess, "Couldn't initialize application...")

    mApp.run() match {
      case Left(err: WasabiError) => isSuccess = false
      case Right(succ: Unit) => isSuccess = true
    }
    assert(!isSuccess, "run() should return an ERROR if processor returns an ERROR...")

    //app.stop()
  }
}
