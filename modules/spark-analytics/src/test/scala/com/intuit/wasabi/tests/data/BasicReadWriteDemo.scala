package com.intuit.wasabi.tests.data

import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SQLContext, SaveMode}
import org.apache.spark.{SparkConf, SparkContext}
;

/**
  * Created by nbarge on 9/25/16.
  */
object BasicReadWriteDemo {

  def main(args: Array[String]) {

    val CassandraHost = "127.0.0.1"

    // Tell Spark the address of one Cassandra node:
    val conf = new SparkConf(true)
      .set("spark.cassandra.connection.host", CassandraHost)
      .set("spark.cleaner.ttl", "3600")
      .setMaster("local")
      .setAppName(getClass.getSimpleName)

    // Connect to the Spark cluster:
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    val cSqlContext = new CassandraSQLContext(sc)

    println("Reading cassandra table using CassandraSQLContext")
    val df1 = cSqlContext.cassandraSql("select * from test.key_value")
    df1.show()

    val CassandraFormat = "org.apache.spark.sql.cassandra"
    val testTableOps = Map( "table" -> "key_value", "keyspace" -> "test" )
    println("Reading cassandra table using SQLContext - option#1 - load DataFrame")
    val df2 = sqlContext.read.format(CassandraFormat).options(testTableOps).load()
    df2.show()

    println("Reading cassandra table using SQLContext - option#2 - create a spark temp table")
    sqlContext.sql("CREATE TEMPORARY TABLE key_value_temp USING "+CassandraFormat+" OPTIONS (table \"key_value\", keyspace \"test\", pushdown \"true\")")
    val df3 = sqlContext.sql("select * from key_value_temp")
    df3.show()

    println("Appending a random row to Cassandra table using SparkAPI")
    val random = scala.util.Random
    val keyId = random.nextInt()
    val newDataRdd = sc.parallelize(Seq(Row(keyId, keyId+" row")))
    val schema = StructType(Array(StructField("key",IntegerType),StructField("value",StringType)))
    val df4 = sqlContext.createDataFrame(newDataRdd, schema)
    df4.write.format(CassandraFormat).mode(SaveMode.Append).options(testTableOps).save()

    println("Inserting a random row to Cassandra table using CassandraConnector & session")
    val cConn = CassandraConnector(conf)
    val cSession = cConn.openSession();
    val keyId2 = random.nextInt()
    cSession.execute("INSERT INTO test.key_value(key, value) VALUES ("+keyId2+", '"+keyId2+" row')")

    println("Stopping the Spark context.")

    sc.stop()
  }
}
