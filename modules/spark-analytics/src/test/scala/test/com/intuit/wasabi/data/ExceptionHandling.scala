package com.intuit.wasabi.data

import cats.data.Xor
import cats.data.Xor.{Left, Right, left, right}
import com.holdenkarau.spark.testing.SharedSparkContext
import com.intuit.wasabi.data.exception.{ApplicationException, WasabiError}
import com.intuit.wasabi.data.util.Utility
import org.scalatest.FunSuite

/**
  * Created by nbarge on 10/14/16.
  */
class ExceptionHandling extends FunSuite with SharedSparkContext {

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
    result2 match {
      case scala.util.Left(error:WasabiError.ApplicationError)  => println(s"#2 Error Message => ${error.detail}, Error Exception => ${error.detail.getStackTraceString}")
      case scala.util.Right(num:Integer)  => println(s"#2 Integer => $num")
    }
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

    val iList = Seq("123","345","567","bad input 1","bad input 2","667","888");
    val strRdd = sc.parallelize(iList)

    val intRdd = strRdd.map(Utility.parse)

    println("Print Left values")

    println("Print Right values")
    intRdd.map(x => println(x))  }
}
