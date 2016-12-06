package com.intuit.wasabi.sparkanalytics.data.conf.guice

import com.datastax.spark.connector.cql.CassandraConnector
import com.google.inject.name.Names
import com.google.inject.{Guice, Key}
import com.holdenkarau.spark.testing.SharedSparkContext
import com.intuit.wasabi.data.app.{ExecuteSqlApplication, MigrateDataApplication, PopulateDataApplication}
import com.intuit.wasabi.data.conf.AppConfig
import com.intuit.wasabi.data.conf.guice.executesql.ExecuteSqlApplicationDI
import com.intuit.wasabi.data.conf.guice.migratedata.MigrateDataApplicationDI
import com.intuit.wasabi.data.conf.guice.populatedata.PopulateDataApplicationDI
import com.intuit.wasabi.data.processor.migratedata.CopyTableMigrationProcessor
import com.intuit.wasabi.data.repository.{DataStoreConnectionProperties, SparkDataStoreRepository}
import com.typesafe.config.ConfigFactory
import org.apache.spark.Logging
import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
  * Created by nbarge on 11/21/16.
  */

@RunWith(classOf[JUnitRunner])
class ApplicationInjectors extends FunSuite with SharedSparkContext with MockFactory with Logging {
  var setting: AppConfig = null

  override def beforeAll() {
    super.beforeAll()

    val jMap: java.util.Map[String,String] = new java.util.HashMap[String,String]()
    //jMap.put("app_id", appId)
    jMap.put("master","local[2]")

    setting = new AppConfig(Option.apply(ConfigFactory.parseMap(jMap)))
  }

  test("MigrateDataApplication object tree") {
    val appId="migrate-data"
    val injector = Guice.createInjector(new MigrateDataApplicationDI(setting.getConfigForApp(appId), Option(sc)))
    val app = injector.getInstance(classOf[MigrateDataApplication])
    assert(app!=null, "MigrateDataApplication instance is NULL...")
    assert(app.copyTableMigrationProcessor!=null, "app.CopyTableMigrationProcessor instance is NULL...")

    val processor = injector.getInstance(classOf[CopyTableMigrationProcessor])
    val sRepository = injector.getInstance(Key.get(classOf[SparkDataStoreRepository], Names.named("SourceDataStoreRepository")))
    val dRepository = injector.getInstance(Key.get(classOf[SparkDataStoreRepository], Names.named("DestinationDataStoreRepository")))
    val dSqlContext = injector.getInstance(Key.get(classOf[CassandraSQLContext], Names.named("DestinationDataStoreCassandraSQLContext")))
    val sSqlContext = injector.getInstance(Key.get(classOf[CassandraSQLContext], Names.named("SourceDataStoreCassandraSQLContext")))
    val dCassandraConnector = injector.getInstance(Key.get(classOf[CassandraConnector], Names.named("DestinationDataStoreCassandraConnector")))
    val sCassandraConnector = injector.getInstance(Key.get(classOf[CassandraConnector], Names.named("SourceDataStoreCassandraConnector")))
    val dDataStoreConnectionProperties = injector.getInstance(Key.get(classOf[DataStoreConnectionProperties], Names.named("DestinationDataStoreConnectionProperties")))
    val sDataStoreConnectionProperties = injector.getInstance(Key.get(classOf[DataStoreConnectionProperties], Names.named("SourceDataStoreConnectionProperties")))

    assert(processor!=null, "CopyTableMigrationProcessor instance is NULL...")
    assert(processor.sRepository!=null, "processor.Source SparkDataStoreRepository instance is NULL...")
    assert(processor.dRepository!=null, "processor.Destination SparkDataStoreRepository instance is NULL...")
    assert(processor.appConfig!=null, "processor.AppConfig instance is NULL...")
    assert(sRepository!=null, "Source SparkDataStoreRepository instance is NULL...")
    assert(dRepository!=null, "Destination SparkDataStoreRepository instance is NULL...")
    assert(dSqlContext!=null, "Destination CassandraSQLContext instance is NULL...")
    assert(sSqlContext!=null, "Source CassandraSQLContext instance is NULL...")
    assert(dCassandraConnector!=null, "DestinationDataStoreCassandraConnector instance is NULL...")
    assert(sCassandraConnector!=null, "SourceDataStoreCassandraConnector instance is NULL...")
    assert(dDataStoreConnectionProperties!=null, "SourceDataStoreCassandraConnector instance is NULL...")
    assert(sDataStoreConnectionProperties!=null, "SourceDataStoreCassandraConnector instance is NULL...")

  }

  test("ExecuteSqlApplication object tree") {
    val appId="execute-sql"
    val injector = Guice.createInjector(new ExecuteSqlApplicationDI(setting.getConfigForApp(appId), Option(sc)))

    val app = injector.getInstance(classOf[ExecuteSqlApplication])
    val repository = injector.getInstance(Key.get(classOf[SparkDataStoreRepository], Names.named("DataStoreCassandraRepository")))
    val sqlContext = injector.getInstance(Key.get(classOf[CassandraSQLContext], Names.named("DataStoreCassandraSQLContext")))
    val cassandraConnector = injector.getInstance(Key.get(classOf[CassandraConnector], Names.named("DataStoreCassandraConnector")))
    val dataStoreConnectionProperties = injector.getInstance(Key.get(classOf[DataStoreConnectionProperties], Names.named("DataStoreConnectionProperties")))

    assert(app!=null, "ExecuteSqlApplication instance is NULL...")
    assert(app.repository!=null, "app.repository instance is NULL...")
    assert(repository!=null, "SparkDataStoreRepository instance is NULL...")
    assert(sqlContext!=null, "sqlContext instance is NULL...")
    assert(cassandraConnector!=null, "cassandraConnector instance is NULL...")
    assert(dataStoreConnectionProperties!=null, "dataStoreConnectionProperties instance is NULL...")

  }

  test("PopulateDataApplication object tree") {
    val appId="populate-data"

    val injector = Guice.createInjector(new PopulateDataApplicationDI(setting.getConfigForApp(appId), Option(sc)))

    val app = injector.getInstance(classOf[PopulateDataApplication])
    val repository = injector.getInstance(Key.get(classOf[SparkDataStoreRepository], Names.named("DataStoreCassandraRepository")))
    val sqlContext = injector.getInstance(Key.get(classOf[CassandraSQLContext], Names.named("DataStoreCassandraSQLContext")))
    val cassandraConnector = injector.getInstance(Key.get(classOf[CassandraConnector], Names.named("DataStoreCassandraConnector")))
    val dataStoreConnectionProperties = injector.getInstance(Key.get(classOf[DataStoreConnectionProperties], Names.named("DataStoreConnectionProperties")))

    assert(app!=null, "PopulateDataApplication instance is NULL...")
    assert(app.repository!=null, "app.repository instance is NULL...")
    assert(repository!=null, "SparkDataStoreRepository instance is NULL...")
    assert(sqlContext!=null, "sqlContext instance is NULL...")
    assert(cassandraConnector!=null, "cassandraConnector instance is NULL...")
    assert(dataStoreConnectionProperties!=null, "dataStoreConnectionProperties instance is NULL...")

  }
}
