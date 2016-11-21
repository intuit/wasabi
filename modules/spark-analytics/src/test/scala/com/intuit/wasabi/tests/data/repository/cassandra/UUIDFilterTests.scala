package com.intuit.wasabi.tests.data.repository.cassandra

import java.sql.{Date, Timestamp}
import java.util
import java.util.UUID

import cats.data.Xor
import cats.data.Xor._
import com.datastax.spark.connector.cql.CassandraConnector
import com.google.inject.Guice
import com.holdenkarau.spark.testing.SharedSparkContext
import com.intuit.wasabi.tests.data.GroupConcatTest._
import com.intuit.wasabi.data.app.MigrateDataApplication
import com.intuit.wasabi.data.conf.AppConfig
import com.intuit.wasabi.data.conf.guice.migratedata.MockCassandraSQLContext
import com.intuit.wasabi.data.exception.{ProcessorException, WasabiError}
import com.intuit.wasabi.data.processor.migratedata.CopyTableMigrationProcessor
import com.intuit.wasabi.data.util.Constants._
import com.typesafe.config.ConfigFactory
import org.apache.spark.{Logging, SparkConf, SparkContext}
import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.apache.spark.sql.types.{StringType, StructField, StructType, TimestampType}
import org.scalamock.scalatest.MockFactory
import org.scalatest.FunSuite

/**
  * Created by nbarge on 10/21/16.
  */
class UUIDFilterTests extends FunSuite with SharedSparkContext with MockFactory with Logging {

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

  test("Filter with existing UUID") {

    import org.apache.spark.sql._
    val sqlCtx = new SQLContext(sc)
    val testUUID = UUID.fromString("ea830e07-baff-40f3-b322-7c6d8742df7f")

    val session = CassandraConnector(sc.getConf).openSession()
    session.execute("CREATE KEYSPACE IF NOT EXISTS test_ks WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '3'}  AND durable_writes = true")
    session.execute("DROP TABLE IF EXISTS test_ks.test_uuid_tbl1 ")
    session.execute("CREATE TABLE test_ks.test_uuid_tbl1 (id uuid, name1 text, name2 text, PRIMARY KEY (id))")
    session.execute(s"INSERT INTO test_ks.test_uuid_tbl1(id,name1,name2) VALUES($testUUID,'test1','test2')")

    val uuidTestDF = sqlCtx.read.format("org.apache.spark.sql.cassandra").options(Map("keyspace"->"test_ks","table"->"test_uuid_tbl1")).load
    uuidTestDF.map(r => r.toString()).foreach(s => println(s))

    uuidTestDF.filter("id = 'ea830e07-baff-40f3-b322-7c6d8742df7f'").show()

/*
    uuidTestDF.registerTempTable("UUIDTestTbl")
    val sqlQ = s"SELECT * FROM UUIDTestTbl WHERE id = '$testUUID'"
    println(s"Now running: sqlQ=$sqlQ")

    val filteredDF = sqlCtx.sql(sqlQ)

    println(filteredDF.queryExecution)
    filteredDF.show*/

  }

  test("Filter with existing UUID - DataStax Example") {

    val sqlCtx = new SQLContext(sc)

    import sqlCtx.implicits._
    import org.apache.spark.sql._

    val testUUID = UUID.fromString("b574a408-4402-4335-a082-a4e35dc7b026")

    val tblDDL = "CREATE TABLE t29242.tester (id uuid PRIMARY KEY, name map<text, text>) WITH bloom_filter_fp_chance = 0.01 AND caching = '{\"keys\":\"ALL\", \"rows_per_partition\":\"NONE\"}' AND comment = '' AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'} AND compression = {'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'} AND dclocal_read_repair_chance = 0.1 AND default_time_to_live = 0 AND gc_grace_seconds = 864000 AND max_index_interval = 2048 AND memtable_flush_period_in_ms = 0 AND min_index_interval = 128 AND read_repair_chance = 0.0 AND speculative_retry = '99.0PERCENTILE';"

    val session = CassandraConnector(sc.getConf).openSession()
    session.execute("CREATE KEYSPACE IF NOT EXISTS t29242 WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '3'}  AND durable_writes = true")
    session.execute("DROP TABLE IF EXISTS t29242.tester ")
    session.execute(tblDDL)
    session.execute(s"INSERT INTO t29242.tester (id, name ) VALUES ( 5880013c-2086-48ae-ba6e-20546b4283b0, { 'Joey' : 'Martinez Torres' })")
    session.execute(s"INSERT INTO t29242.tester (id, name ) VALUES ( b574a408-4402-4335-a082-a4e35dc7b026, { 'Jerry' : 'Martinez Torres'}) ;")

    val uuidTestDF = sqlCtx.read.format("org.apache.spark.sql.cassandra").options(Map("keyspace"->"t29242","table"->"tester")).load
    uuidTestDF.map(r => r.toString()).foreach(s => println(s))

    uuidTestDF.filter("id = 'b574a408-4402-4335-a082-a4e35dc7b026'").show()

  }

  test("Read a big partition - can read be concurrent") {
    val sqlCtx = new CassandraSQLContext(sc)
    val sqlQ = s"SELECT count(*) FROM test_uuid_ks.test_uuid_tbl1"
    println(s"Now running: sqlQ=$sqlQ")
    val df = sqlCtx.sql(sqlQ)
    df.show()
  }

  test("For loop") {
    val list = Seq("Hi","Hello")
    var i=0
    val list2:scala.collection.mutable.ArraySeq[String] = new scala.collection.mutable.ArraySeq(2)
    for(r <- list) {
      list2(i) = r + "Nilesh"
      i = i +1
    }

    for(r <- list2) println(r)
  }

  def main(args: Array[String]): Unit = {
    val list = Seq("Hi","Hello")
    var i=0
    val list2:scala.collection.mutable.ArraySeq[String] = new scala.collection.mutable.ArraySeq(2)
    for(r <- list) {
      list2(i) = r + "Nilesh"
      i = i +1
    }

    for(r <- list2) println(r)
  }
}

final case class TestUUIDSchema(id: String, name1: String, name2: String) extends Serializable

object UUIDTestDataStaxExample {
  def main(args: Array[String]): Unit = {
  /*  val conf = new SparkConf(true).setAppName(getClass.getSimpleName) //.setMaster("local")

    val sc = new SparkContext(conf)
    val sqlContext = new CassandraSQLContext(sc)
    //sqlContext.setKeyspace("t29242")
    sqlContext.setKeyspace("jabba_experiments")

    //val testUUID = UUID.fromString("b574a408-4402-4335-a082-a4e35dc7b026")
    val testUUID = UUID.fromString("d6f0c535-5dc3-4b26-b349-5b3f8e014580")

    val tblDDL = "CREATE TABLE t29242.tester (id uuid PRIMARY KEY, name map<text, text>) WITH bloom_filter_fp_chance = 0.01 AND caching = '{\"keys\":\"ALL\", \"rows_per_partition\":\"NONE\"}' AND comment = '' AND compaction = {'class': 'org.apache.cassandra.db.compaction.SizeTieredCompactionStrategy'} AND compression = {'sstable_compression': 'org.apache.cassandra.io.compress.LZ4Compressor'} AND dclocal_read_repair_chance = 0.1 AND default_time_to_live = 0 AND gc_grace_seconds = 864000 AND max_index_interval = 2048 AND memtable_flush_period_in_ms = 0 AND min_index_interval = 128 AND read_repair_chance = 0.0 AND speculative_retry = '99.0PERCENTILE';"

    val session = CassandraConnector(sc.getConf).openSession()
    session.execute("CREATE KEYSPACE IF NOT EXISTS t29242 WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '3'}  AND durable_writes = true")

    /*session.execute("DROP TABLE IF EXISTS t29242.tester ")
    session.execute(tblDDL)
    session.execute(s"INSERT INTO t29242.tester (id, name ) VALUES ( 5880013c-2086-48ae-ba6e-20546b4283b0, { 'Joey' : 'Martinez Torres' })")
    session.execute(s"INSERT INTO t29242.tester (id, name ) VALUES ( b574a408-4402-4335-a082-a4e35dc7b026, { 'Jerry' : 'Martinez Torres'}) ;")

    val sqlQ = s"SELECT count(*) FROM tester WHERE id = '$testUUID'"
    println(s"Now running: sqlQ=$sqlQ")
    val filteredDF = sqlContext.sql(sqlQ)
    println(filteredDF.queryExecution)
    filteredDF.show*/

    session.execute("DROP TABLE IF EXISTS t29242.test_uuid_tbl1 ")
    session.execute("CREATE TABLE t29242.test_uuid_tbl1 (id uuid, name1 text, name2 text, PRIMARY KEY (id))")
    session.execute(s"INSERT INTO t29242.test_uuid_tbl1(id,name1,name2) VALUES($testUUID,'test1','test2')")

    //val sqlQ2 = s"SELECT count(*) FROM test_uuid_tbl1 WHERE id = '$testUUID'"
    val sqlQ2 = s"SELECT count(*) FROM user_assignment_2 WHERE experiment_id = '$testUUID'"
    println(s"Now running: sqlQ=$sqlQ2")
    val filteredDF2 = sqlContext.sql(sqlQ2)
    println(filteredDF2.queryExecution)
    filteredDF2.show

    sc.stop()*/

    val list = Seq("Hi","Hello")
    var i=0
    val list2:scala.collection.mutable.ArraySeq[String] = new scala.collection.mutable.ArraySeq(2)
    for(r <- list) {
      list2(i) = r + " Nilesh"
      i = i + 1
    }

    for(r <- list2.iterator) println(r)
  }
}
