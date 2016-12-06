package com.intuit.wasabi.tests.data.repository.cassandra

import com.datastax.spark.connector.cql.CassandraConnector
import com.holdenkarau.spark.testing.SharedSparkContext
import com.intuit.wasabi.data.conf.AppConfig
import com.intuit.wasabi.data.util.Constants
import com.typesafe.config.ConfigFactory
import org.apache.spark.Logging
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SQLContext, SaveMode}
import org.junit.runner.RunWith
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
  * Created by nbarge on 10/21/16.
  */

@RunWith(classOf[JUnitRunner])
class SaveModeTest extends FunSuite with SharedSparkContext with MockFactory with Logging {

  var setting: AppConfig = null

  override def beforeAll() {
    super.beforeAll()
    log.info("Setting app_id in the VM arguments...")
    println("******************** Setting app_id in the VM arguments *******************")

    val jMap: java.util.Map[String,String] = new java.util.HashMap[String,String]()
    jMap.put("app_id", "migrate-data")
    jMap.put("master","local[2]")

    setting = new AppConfig(Option.apply(ConfigFactory.parseMap(jMap)))

    val session = CassandraConnector(sc.getConf).openSession()
    session.execute("CREATE KEYSPACE IF NOT EXISTS test_ks WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '3'}  AND durable_writes = true")
    session.execute("DROP TABLE IF EXISTS test_ks.test_save_modes")
    session.execute("CREATE TABLE test_ks.test_save_modes (id int, name text, city text, PRIMARY KEY (id, name)) WITH CLUSTERING ORDER BY (name ASC)")

    session.execute(s"INSERT INTO test_ks.test_save_modes(id,name,city) VALUES(1,'Joel','San Jose')")
    session.execute(s"INSERT INTO test_ks.test_save_modes(id,name,city) VALUES(2,'Alice','San Francisco')")
    session.execute(s"INSERT INTO test_ks.test_save_modes(id,name,city) VALUES(3,'June','San Jose')")
  }

  test("Overwrite mode test") {

    val sqlCtx = new SQLContext(sc)

    val tblOpts = Map("keyspace"->"test_ks","table"->"test_save_modes")
    println("Existing Data")
    val existingDataDF = sqlCtx.read.format(Constants.SPARK_SQL_CASSANDRA_FORMAT).options(tblOpts).load
    existingDataDF.map(r => r.toString()).foreach(s => println(s))

    println("New Data - Overwrite")
    val rowsRDD = sc.parallelize(Seq(Row(3,"June","San Francisco"),Row(4,"Mike","Sunnyvale")))
    val schema = StructType(Seq(StructField("id", DataTypes.IntegerType), StructField("name", DataTypes.StringType), StructField("city", DataTypes.IntegerType)))
    val newDataDF = sqlCtx.createDataFrame(rowsRDD, schema)
    newDataDF.write.format(Constants.SPARK_SQL_CASSANDRA_FORMAT).options(tblOpts).mode(SaveMode.Overwrite).save()
    newDataDF.map(r => r.toString()).foreach(s => println(s))

    println("Updated Data")
    val updatedDataDF = sqlCtx.read.format("org.apache.spark.sql.cassandra").options(Map("keyspace"->"test_ks","table"->"test_save_modes")).load
    updatedDataDF.map(r => r.toString()).foreach(s => println(s))
  }

  test("Append mode test") {

    val sqlCtx = new SQLContext(sc)

    val tblOpts = Map("keyspace"->"test_ks","table"->"test_save_modes")
    println("Existing Data")
    val existingDataDF = sqlCtx.read.format(Constants.SPARK_SQL_CASSANDRA_FORMAT).options(tblOpts).load
    existingDataDF.map(r => r.toString()).foreach(s => println(s))

    println("New Data - Append")
    val rowsRDD = sc.parallelize(Seq(Row(3,"June","San Francisco"),Row(4,"Mike","Sunnyvale")))
    val schema = StructType(Seq(StructField("id", DataTypes.IntegerType), StructField("name", DataTypes.StringType), StructField("city", DataTypes.IntegerType)))
    val newDataDF = sqlCtx.createDataFrame(rowsRDD, schema)
    newDataDF.write.format(Constants.SPARK_SQL_CASSANDRA_FORMAT).options(tblOpts).mode(SaveMode.Append).save()
    newDataDF.map(r => r.toString()).foreach(s => println(s))

    println("Updated Data")
    val updatedDataDF = sqlCtx.read.format("org.apache.spark.sql.cassandra").options(Map("keyspace"->"test_ks","table"->"test_save_modes")).load
    updatedDataDF.map(r => r.toString()).foreach(s => println(s))
  }

  test("Ignore mode test") {

    val sqlCtx = new SQLContext(sc)

    val tblOpts = Map("keyspace"->"test_ks","table"->"test_save_modes")
    println("Existing Data")
    val existingDataDF = sqlCtx.read.format(Constants.SPARK_SQL_CASSANDRA_FORMAT).options(tblOpts).load
    existingDataDF.map(r => r.toString()).foreach(s => println(s))

    println("New Data - Ignore")
    val rowsRDD = sc.parallelize(Seq(Row(3,"June","San Francisco"),Row(4,"Mike","Sunnyvale")))
    val schema = StructType(Seq(StructField("id", DataTypes.IntegerType), StructField("name", DataTypes.StringType), StructField("city", DataTypes.IntegerType)))
    val newDataDF = sqlCtx.createDataFrame(rowsRDD, schema)
    newDataDF.write.format(Constants.SPARK_SQL_CASSANDRA_FORMAT).options(tblOpts).mode(SaveMode.Ignore).save()
    newDataDF.map(r => r.toString()).foreach(s => println(s))

    println("Updated Data")
    val updatedDataDF = sqlCtx.read.format("org.apache.spark.sql.cassandra").options(Map("keyspace"->"test_ks","table"->"test_save_modes")).load
    updatedDataDF.map(r => r.toString()).foreach(s => println(s))
  }
}



