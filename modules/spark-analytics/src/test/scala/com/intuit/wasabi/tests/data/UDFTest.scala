package com.intuit.wasabi.tests.data

import java.sql.Timestamp
import java.time.{Instant, ZoneId}

import com.holdenkarau.spark.testing.SharedSparkContext
import com.intuit.wasabi.data.udf.CurrentTimestampFunc
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SQLContext}
import org.scalatest.FunSuite


/**
  * Created by nbarge on 9/27/16.
  */
class UDFTest extends FunSuite with SharedSparkContext {

  test("CurrentTimestampFunc: create and use  UDF - success") {
    val sqlContext = new SQLContext(sc)

    val newDataRdd = sc.parallelize(Seq(Row("Hello1", "HelloValue1"),Row("Hello2", "HelloValue2")))
    val schema = StructType(Array(StructField("key",StringType),StructField("value",StringType)))
    val df1 = sqlContext.createDataFrame(newDataRdd, schema)
    df1.registerTempTable("TempTbl1")

    val myStrLenFunc = (s: String) => s.length()
    sqlContext.udf.register("myStrLen", myStrLenFunc)

    val currentTimestampFunc = new CurrentTimestampFunc()
    sqlContext.udf.register("wasabi_current_timestamp", currentTimestampFunc)

    val df2 = sqlContext.sql("SELECT key, myStrLen(key) as key_length, value, wasabi_current_timestamp('GMT') current_ts FROM TempTbl1")
    df2.show(10)

  }
}
