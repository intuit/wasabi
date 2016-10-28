package com.intuit.wasabi.data

import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}
;

/**
  * Created by nbarge on 9/27/16.
  */
object GroupConcatTest {
  def main(args: Array[String]) {

    val conf = new SparkConf(true)
      .set("spark.cassandra.connection.host", "localhost")
      .set("spark.ui.port", "4041")
      .set("spark.driver.host", "localhost")
      .set("spark.driver.port", "4040")
      .set("spark.port.maxRetries","5")
      .setMaster("local")
      .setAppName(getClass.getSimpleName)

    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    val dataRdd = sc.parallelize(Seq(
      Row("San Jose", "Nilesh"),
      Row("San Jose", "John"),
      Row("San Francisco", "Kevin")
    ))

    val schema = StructType(Array(StructField("city_name",StringType),StructField("people",StringType)))

    val df1 = sqlContext.createDataFrame(dataRdd, schema)
    df1.show()

/*    val df2 = df1.groupBy("city_name")
       .agg(IdeaGroupConcat(col("people")).as("peoples"))

    df2.show()*/

    sc.stop()
  }
}
