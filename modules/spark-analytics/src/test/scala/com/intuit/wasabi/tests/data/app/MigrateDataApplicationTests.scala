package com.intuit.wasabi.tests.data.app

import java.sql.Timestamp

import cats.data.Xor
import cats.data.Xor._
import com.google.inject.Guice
import com.holdenkarau.spark.testing.SharedSparkContext
import com.intuit.wasabi.data.app.MigrateDataApplication
import com.intuit.wasabi.data.conf.AppConfig
import com.intuit.wasabi.data.exception.{ProcessorException, WasabiError}
import com.intuit.wasabi.data.processor.migratedata.CopyTableMigrationProcessor
import com.intuit.wasabi.tests.data.conf.guice.migratedata.{MockCassandraSQLContext, MockMigrateDataApplicationDI}
import com.typesafe.config.ConfigFactory
import org.apache.spark.Logging
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{StringType, StructField, StructType, TimestampType}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite

/**
  * Created by nbarge on 10/21/16.
  */
class MigrateDataApplicationTests extends FunSuite with SharedSparkContext with MockFactory with Logging {

  var setting: AppConfig = null

  override def beforeAll() {
    super.beforeAll()
    log.info("Setting app_id in the VM arguments...")
    println("******************** Setting app_id in the VM arguments *******************")

    val jMap: java.util.Map[String,String] = new java.util.HashMap[String,String]()
    jMap.put("app_id", "migrate-data")
    jMap.put("master","local[2]")
    jMap.put("default.spark.spark.driver.allowMultipleContexts","true")

    setting = new AppConfig(Option.apply(ConfigFactory.parseMap(jMap)))
  }

  test("Copy empty table") {
    val appConfig = setting.getConfigForApp("migrate-data")
    val injector = Guice.createInjector(new MockMigrateDataApplicationDI(appConfig, sc))
    val mApp = injector.getInstance(classOf[MigrateDataApplication])

    var mockSqlContext: MockCassandraSQLContext = null

    mApp.copyTableMigrationProcessor.sRepository.getSqlContext() match {
      case Left(error: WasabiError) => {}
      case Right(sqlContext: MockCassandraSQLContext) => mockSqlContext=sqlContext
      case _ => log.error("Couldn't find right match for CassandraSQLContext... ")
    }
    assert(mockSqlContext!=null, "MockCassandraSQLContext can not be NULL...")

    val rdd = sc.parallelize(Seq(Row()))
    val schema = StructType(Array(StructField("experiment_id",StringType),StructField("user_id",StringType),StructField("context",StringType),StructField("bucket_label",StringType),StructField("created",TimestampType)))

    val df1 = mockSqlContext.createDataFrame(rdd, schema)
    df1.show()

    mockSqlContext.mock("SELECT experiment_id,user_id,context,bucket_label,created FROM user_assignment", df1)

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
    assert(isSuccess, "Run should finish successfully for empty table...")
  }

  test("Copy table with some data") {
    val appConfig = setting.getConfigForApp("migrate-data")
    val injector = Guice.createInjector(new MockMigrateDataApplicationDI(appConfig, sc))
    val mApp = injector.getInstance(classOf[MigrateDataApplication])

    var mockSqlContext: MockCassandraSQLContext = null

    mApp.copyTableMigrationProcessor.sRepository.getSqlContext() match {
      case Left(error: WasabiError) => {}
      case Right(sqlContext: MockCassandraSQLContext) => mockSqlContext=sqlContext
      case _ => log.error("Couldn't find right match for CassandraSQLContext... ")
    }
    assert(mockSqlContext!=null, "MockCassandraSQLContext can not be NULL...")

    val rdd = sc.parallelize(Seq(Row("test_exp1","test_user_id1","text_context1","test_bucket_label1",new Timestamp(System.currentTimeMillis()))))
    val schema = StructType(Array(StructField("experiment_id",StringType),StructField("user_id",StringType),StructField("context",StringType),StructField("bucket_label",StringType),StructField("created",TimestampType)))

    val df1 = mockSqlContext.createDataFrame(rdd, schema)
    df1.show()

    mockSqlContext.mock("SELECT experiment_id,user_id,context,bucket_label,created FROM user_assignment", df1)

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
    assert(isSuccess, "Run should finish successfully for non-empty table...")
  }

  test("Processor works fine...") {
    val appConfig = setting.getConfigForApp("migrate-data")
    val injector = Guice.createInjector(new MockMigrateDataApplicationDI(appConfig, sc))
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
    val injector = Guice.createInjector(new MockMigrateDataApplicationDI(appConfig, sc))
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
