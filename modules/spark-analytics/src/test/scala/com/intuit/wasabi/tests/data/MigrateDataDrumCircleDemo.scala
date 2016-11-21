package com.intuit.wasabi.tests.data

import java.io.FileInputStream
import java.util.Properties

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by nbarge on 9/27/16.
  */
object MigrateDataDrumCircleDemo {

  def prepareSparkConfig() :  SparkConf = {
    println("prepareSparkConfig - started")
    val conf = new SparkConf(true);
    val prop = new Properties()
    val fileInputStream = new FileInputStream("config.properties")

    prop.load(fileInputStream)

    //prop.stringPropertyNames().forEach(name => conf.set(name, prop.getProperty(prop)))

    val value = ConfigFactory.load("config.properties")

    conf
  }

  def main(args: Array[String]) {
    val CassandraHost = "localhost"

    val srcCluster = "default"
    val srcKeySpace = "jabba_experiments"
    val srcTable = "experiment"

    val destCluster = "default"
    val destKeySpace = "jabba_experiments_v2"
    val destTable = "experiment"

    // Tell Spark the address of one Cassandra node:
    val conf = new SparkConf(true).setAppName(getClass.getSimpleName)

    // Connect to the Spark cluster:
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    val CASSANDRA_FORMAT = "org.apache.spark.sql.cassandra"
    val srcTableOps = Map( "table" -> srcTable, "keyspace" -> srcKeySpace, "spark.cassandra.connection.host" -> "10.82.124.90", "spark.cassandra.connection.port" -> "9042" )
    //val destTableOps = Map( "table" -> destTable, "keyspace" -> destKeySpace, "spark.cassandra.connection.host" -> "10.82.124.90", "spark.cassandra.connection.port" -> "9042" )

    println("Reading DataFrame from cassandra table using SQLContext: ")
    val srcDF = sqlContext.read.format(CASSANDRA_FORMAT).options(srcTableOps).load()
    srcDF.show(10)

    println("Migrate DataFrame (if needed): ")
    val destDF = srcDF;

    println("Writing DataFrame to cassandra table using SQLContext: ")
    //destDF.write.format(CASSANDRA_FORMAT).mode(SaveMode.Append).options(destTableOps).save()

    sc.stop()
    println("Done: Spark context stopped...")
  }
}
