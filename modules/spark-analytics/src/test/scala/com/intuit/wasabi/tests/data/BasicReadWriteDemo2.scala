package com.intuit.wasabi.tests.data

import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.sql.cassandra.CassandraSQLContext
import org.apache.spark.sql.types.{IntegerType, StructField, StructType}
import org.apache.spark.{SparkConf, SparkContext}
;

/**
  * Created by nbarge on 9/25/16.
  */
object BasicReadWriteDemo2 {

  def main(args: Array[String]) {

    val CassandraHost = "127.0.0.1"

    // Tell Spark the address of one Cassandra node:
    val conf = new SparkConf(true)
      .set("spark.cassandra.connection.host", CassandraHost)
      .set("spark.cleaner.ttl", "3600")
      .set("spark.ui.port", "4045")
      .setMaster("local")
      .setAppName(getClass.getSimpleName)

    // Connect to the Spark cluster:
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    val cSqlContext = new CassandraSQLContext(sc)

    val newDataRdd = sc.parallelize(Seq(Row(1, 2, 3),Row(11, 12, 13)))
    val schema = StructType(Array(StructField("c1",IntegerType),StructField("c2",IntegerType),StructField("c3",IntegerType)))
    val df_t1 = sqlContext.createDataFrame(newDataRdd, schema)

    val newDataRdd2 = sc.parallelize(Seq(Row(1, 2, 3),Row(11, 12, 13)))
    val schema2 = StructType(Array(StructField("c1",IntegerType),StructField("c4",IntegerType),StructField("c5",IntegerType)))
    val df_t2 = sqlContext.createDataFrame(newDataRdd, schema)

    val newDataRdd3 = sc.parallelize(Seq(Row(1, 2, 3),Row(11, 12, 13)))
    val schema3 = StructType(Array(StructField("c2",IntegerType),StructField("c6",IntegerType),StructField("c7",IntegerType)))
    val df_t3 = sqlContext.createDataFrame(newDataRdd, schema)

    val df_t2_t1 = df_t2.join((df_t1), "c1")
    val df_t3_t1 = df_t2.join((df_t1), "c2")


    println("Stopping the Spark context.")

    sc.stop()
  }
}
