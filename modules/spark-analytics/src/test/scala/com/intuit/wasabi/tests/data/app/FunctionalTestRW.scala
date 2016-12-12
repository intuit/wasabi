package com.intuit.wasabi.tests.data.app

import com.google.inject.name.Names
import com.google.inject.{Guice, Key}
import com.intuit.wasabi.data.conf.AppConfig
import com.intuit.wasabi.data.conf.guice.migratedata.MigrateDataApplicationDI
import com.intuit.wasabi.data.exception.WasabiError
import com.intuit.wasabi.data.repository.SparkDataStoreRepository
import com.typesafe.config.ConfigFactory
import org.apache.spark.{SparkConf, SparkContext, SparkUtils}
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.sql.cassandra.CassandraSQLContext
import cats.data.Xor
import cats.data.Xor._
import org.apache.log4j.{Level, Logger}

/**
  * Created by nbarge on 12/10/16.
  */
object FunctionalTestRW {
  def main(args: Array[String]) {

    val host = args(0)
    val port = args(1)
    val sql = args(2)
    //setLogLevels(Level.WARN, Seq("org.apache.spark", "org", "akka"))
    silenceSpark()

    //simple1(host, port, sql)
    simple2(host, port, sql)
  }

  import org.apache.log4j.{Level, Logger}
  /**
    * set all loggers to the given log level.  Returns a map of the value of every logger
    * @param level
    * @param loggers
    * @return
    */
  def setLogLevels(level: org.apache.log4j.Level, loggers: TraversableOnce[String]) = {
    loggers.map{
      loggerName =>
        val logger = Logger.getLogger(loggerName)
        val prevLevel = logger.getLevel()
        logger.setLevel(level)
        loggerName -> prevLevel
    }.toMap
  }

  /**
    * turn off most of spark logging.  Returns a map of the previous values so you can turn logging back to its
    * former values
    */
  def silenceSpark() = {
    setLogLevels(Level.WARN, Seq("spark", "org.eclipse.jetty", "akka"))
  }


  def setLogLevels(level: Level, loggers: Seq[String]): Map[String, Level] = loggers.map(loggerName => {
    val logger = Logger.getLogger(loggerName)
    val prevLevel = logger.getLevel
    logger.setLevel(level)
    loggerName -> prevLevel
  }).toMap

  def simple2(host:String, port:String, sql:String): Unit = {
    println("Simple2: STARTED")

    val appId="migrate-data"
    val jMap: java.util.Map[String,String] = new java.util.HashMap[String,String]()
    jMap.put("app_id", appId)
    jMap.put("master", "local[*]")
    //jMap.put("migrate-data.spark.spark.cassandra.connection.host", host)
    //jMap.put("migrate-data.spark.spark.cassandra.connection.port", port)

    val setting = new AppConfig(Option.apply(ConfigFactory.parseMap(jMap)))
    val appConfig = setting.getConfigForApp(appId)

    val injector = Guice.createInjector(new MigrateDataApplicationDI(appConfig))
    val sRepository = injector.getInstance(Key.get(classOf[SparkDataStoreRepository], Names.named("SourceDataStoreRepository")))

    var df:DataFrame = null
    sRepository.read(sql) match {
      case Left(error: WasabiError) => {}
      case Right(tdf: DataFrame) => df=tdf
    }

    df.show()
  }

  def simple1(host:String, port:String, sql:String): Unit = {
    println("Simple1: STARTED")

    // Tell Spark the address of one Cassandra node:
    val conf = new SparkConf(true)
      .set("spark.cassandra.connection.host", host)
      .set("spark.cassandra.connection.port", port)
      .setMaster("local[*]")
      .setAppName(getClass.getSimpleName)

    // Connect to the Spark cluster:
    val sc = new SparkContext(conf)
    val cSqlContext = new CassandraSQLContext(sc)

    println("Reading cassandra table using CassandraSQLContext")
    val df1 = cSqlContext.sql(sql)
    df1.show()

    sc.stop()
  }

}
