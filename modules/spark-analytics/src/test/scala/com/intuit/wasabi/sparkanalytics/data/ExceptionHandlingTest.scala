package com.intuit.wasabi.sparkanalytics.data

import cats.data.Xor.{Left, Right}
import com.holdenkarau.spark.testing.SharedSparkContext
import com.intuit.wasabi.data.exception.WasabiError
import com.intuit.wasabi.data.util.Utility
import org.apache.spark.Logging
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.scalatest.junit.JUnitRunner
import org.slf4j.Logger

/**
  * Created by nbarge on 10/14/16.
  */
@RunWith(classOf[JUnitRunner])
class ExceptionHandlingTest extends FunSuite with SharedSparkContext with BeforeAndAfterAll with Logging {

  override def beforeAll() {
    super.beforeAll()
    sc.setLogLevel("INFO")
  }

  test("Test cas Xor: null input") {
    val result = Utility.parse(null)
    result match {
      case Left(error:WasabiError.ApplicationError) => println(s"Error Message => ${error.detail}, Error Exception => ${error.detail.getStackTraceString}")
      case Right(num:Integer)  => println(s"Integer => $num")
    }

    assert(result.isLeft, true)
    //assert(result.isRight, false)
  }

  test("Test Either: null input") {
    val result2 = Utility.parse2(null)
    var wasThereError=false
    result2 match {
      case scala.util.Left(error:WasabiError.ApplicationError)  => {
        println(s"#2 Error Message => ${error.detail}, Error Exception => ${error.detail.getStackTraceString}")
        wasThereError=true
      }
      case scala.util.Right(num:Integer)  => {
        println(s"#2 Integer => $num")
        wasThereError=false
      }
    }

    assert(wasThereError==true, "There should be error as null is not a valid number")
  }

  test("Test Either: valid input") {
    val result3 = Utility.parse2("123")
    result3 match {
      case scala.util.Left(error:WasabiError.ApplicationError)  => println(s"#2 Error Message => ${error.detail}, Error Exception => ${error.detail.getStackTraceString}")
      case scala.util.Right(num:Integer)  => println(s"#3 Successfully parsed => $num")
    }

    result3.right.foreach(x => println(x))
    result3.left.foreach(x => println(x))

  }

  test("Test Either: valid input as a function on rdd") {

    val iList = Seq("123","345","567","bad input 1","bad input 2","667","888");
    val strRdd = sc.parallelize(iList)

    val resultRdd = strRdd.map(Utility.parse2)
    /*
        val errorRdd1 = resultRdd.map(x => x.left).flatMap(x => x.)
        println("Print errorRdd1 values")
        errorRdd1.foreach(println(_))



        val intRdd1 = resultRdd.map(_.right).map(x => x.get)
        println("Print intRdd1 values")
        intRdd1.foreach(println(_))

        val errorRdd = resultRdd.map(x => x match {
          case scala.util.Left(error:WasabiError.Application)  => error.detail
          case scala.util.Right(num:Integer)  => None
        })

        val intRdd = resultRdd.map(x => x match {
          case scala.util.Left(error:WasabiError.Application)  => None
          case scala.util.Right(num:Integer)  => num
        })

        println("Print Left values")
        errorRdd.foreach(println(_))

        println("Print Right values")
        intRdd.foreach(println(_))
    */

  }

  test("Test cas Xor: valid input as a function on rdd") {

    val iList = Seq("123", "345", "567", "bad input 1", "bad input 2", "667", "888");
    val strRdd = sc.parallelize(iList)

    val intRdd = strRdd.map(Utility.parse)

    println("Print Left values")

    println("Print Right values")
    intRdd.map(x => println(x))
  }
}
