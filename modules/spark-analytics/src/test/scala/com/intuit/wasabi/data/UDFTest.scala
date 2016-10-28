package com.intuit.wasabi.data

import java.sql.Timestamp
import java.time.{Instant, ZoneId}

import com.holdenkarau.spark.testing.SharedSparkContext
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.FunSuite


/**
  * Created by nbarge on 9/27/16.
  */
class UDFTest extends FunSuite with SharedSparkContext {

  test("create and use UDF - success") {
    val sqlContext = new SQLContext(sc)

    val newDataRdd = sc.parallelize(Seq(Row("Hello1", "HelloValue1"),Row("Hello2", "HelloValue2")))
    val schema = StructType(Array(StructField("key",StringType),StructField("value",StringType)))
    val df1 = sqlContext.createDataFrame(newDataRdd, schema)
    df1.registerTempTable("TempTbl1")

    val myStrLenFunc = (s: String) => s.length()
    sqlContext.udf.register("myStrLen", myStrLenFunc)

    val currentTimestampFunc = (iZoneId: String) => {
      var zone: ZoneId = null
      if(iZoneId==null) {
        zone = ZoneId.systemDefault()
      } else {
        zone = ZoneId.of(iZoneId)
      }

      //Current machine timestamp in ZONE time
      val currentZonedDateTime = Instant.now().atZone(zone)

      new Timestamp(currentZonedDateTime.toInstant.toEpochMilli)
    }
    sqlContext.udf.register("wasabi_current_timestamp", currentTimestampFunc)

    //val df2 = df1.select(col("*"), callUDF("myStrLen", DataTypes.IntegerType, col("key")).as("key_length"))

    val df2 = sqlContext.sql("SELECT key, myStrLen(key) as key_length, value, wasabi_current_timestamp('GMT') current_ts FROM TempTbl1")
    df2.show(10)

  }
}


object test{
  class c1(i:Int) {
    implicit val i1:Int = i
  }
  def v1(c:c1) ={
    import java.time.Duration
    import c.i1
    def v2() ={
      val i = Duration.ofDays(1)
    }
  }

  val o:Option[Int] =None
  val o1:Option[Int] = Some(3)
  val o2 = Some(3)
}