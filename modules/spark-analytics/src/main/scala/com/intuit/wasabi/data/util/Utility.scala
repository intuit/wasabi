package com.intuit.wasabi.data.util

import java.util.Map.Entry
import java.util.Properties

import cats.data.Xor
import cats.data.Xor._
import com.intuit.wasabi.data.exception.{ApplicationException, WasabiError}
import com.typesafe.config.{Config, ConfigValue}

import scala.collection.mutable
import scala.reflect.runtime.universe._
import scala.util.control.Breaks._


/**
  * Utility class to provide different helper/utilities methods.
  *
  */
object Utility {

  /**
    * This method is used to parse SparkApplication's input arguments.
    *
    * @param args - all the arguments passed to spark application in the format of string separated by spaces
    * @return a Map of paramName to paramValue based on the convention "--param1Name param1Val --param2Name param2Val ..."
    */
  def parseArguments(args: Array[String]) : Map[String,String] = {
    val argMap:scala.collection.mutable.Map[String,String]=scala.collection.mutable.Map()
    var idx=0
    var argKey:String = null
    breakable {
      for (counter <- 0 until args.length) {
        argKey=args(idx)
        if (argKey != null && argKey.startsWith("--")) {
          if (idx + 1 <= args.length - 1) {
            argMap.put(argKey.substring(2, argKey.length), args(idx + 1))
          }
          idx += 2
        } else {
          idx += 1
        }

        if (idx >= args.length - 1) {
          break;
        }
      }
    }
    argMap.toMap
  }

  /**
    * Utility method to read configuration properties files
    *
    * @param path to the configuration property file
    * @return a map of propery name to its value
    */
  def readConfigFile(path: String) : Map[String,String] = {

    val map:scala.collection.mutable.Map[String,String] = scala.collection.mutable.Map()


    val prop = new Properties()
    prop.load(Utility.getClass.getResourceAsStream(path))

    val itr = prop.stringPropertyNames().iterator()

    while(itr.hasNext) {
      val key = itr.next()
      map.put(key, prop.getProperty(key))
    }

    map.toMap
  }

  def getObjectInstance(clsName: String): AnyRef = {
    val mirror = runtimeMirror(getClass.getClassLoader)
    val module = mirror.staticModule(clsName)
    mirror.reflectModule(module).instance.asInstanceOf[AnyRef]
  }

  def getClassInstance(clsName: String): Any = {
    val mirror = runtimeMirror(getClass.getClassLoader)
    val cls = mirror.classSymbol(Class.forName(clsName))
    val module = cls.companionSymbol.asModule
    mirror.reflectModule(module).instance
  }

  def invokeObjectMethod(objectName: String, methodName: String) = {
    val mirror = runtimeMirror(getClass.getClassLoader)
    val im =mirror.reflect(getObjectInstance(objectName))
    val method = im.symbol.typeSignature.member(newTermName("findByUsername")).asMethod
    im.reflectMethod(method)(methodName)
  }

  def invokeClassMethod(className: String, methodName: String) = {
    val mirror = runtimeMirror(getClass.getClassLoader)
    val im =mirror.reflect(getClassInstance(className))
    val method = im.symbol.typeSignature.member(newTermName("findByUsername")).asMethod
    im.reflectMethod(method)(methodName)
  }

  def configToMap(config: Config): Map[String,String] = {
    val map:scala.collection.mutable.Map[String,String] = mutable.Map()
    val sItr = config.entrySet().iterator()
    var entry:Entry[String, ConfigValue] = null
    while(sItr.hasNext) {
      entry = sItr.next()
      map.put(entry.getKey, config.getString(entry.getKey))
    }
    map.toMap
  }

  /**
    * Utility method to parse and return table name from given SQL statement
    *
    * @param sql sql statement
    * @param withDBName TRUE means should return table name with the dbName. FALSE means return table name alonw without dbName
    * @return parse and return a tableName
    */
  def getTableName(sql: String, withDBName: Boolean): String = {
    val lSql = sql.toLowerCase
    val fromClauseIndex = lSql.indexOf("from")

    val tblNameEndIndex = lSql.indexOf(" ", fromClauseIndex + 5)

    var tblName: String = null
    if (tblNameEndIndex != -1) {
      tblName = lSql.substring(fromClauseIndex + 5, tblNameEndIndex).trim
    } else {
      tblName = lSql.substring(fromClauseIndex).trim
    }

    if (!withDBName) {
      if (tblName.indexOf(".") != -1) {
        tblName = tblName.substring(tblName.indexOf(".") + 1)
      }
    }

    tblName
  }

  def main(args: Array[String]): Unit = {

    /*
    val sql = "Select * from wasabi.exp where 1=1"
    println("tblName="+getTableName(sql, true))
  */


  }


  def parse(text: String): WasabiError.ApplicationError Xor Integer  = {
    try {
      right(text.toInt)
    } catch {
      case nfe: NumberFormatException => left(WasabiError.ApplicationError(new ApplicationException(s"${text} is not a valid integer.", nfe)))
      case ex: Exception => left(WasabiError.ApplicationError(new ApplicationException(s"${text} is not a valid integer.", ex)))
    }
  }

  def parse2(text: String): Either[WasabiError.ApplicationError, Integer]  = {
    try {
      scala.util.Right(text.toInt)
    } catch {
      case nfe: NumberFormatException => scala.util.Left(WasabiError.ApplicationError(new ApplicationException(s"${text} is not a valid integer.", nfe)))
      case ex: Exception => scala.util.Left(WasabiError.ApplicationError(new ApplicationException(s"${text} is not a valid integer.", ex)))
    }
  }

  def parse3(text: String): Either[WasabiError.ApplicationError, Integer]  = {
    try {
      scala.util.Right(text.toInt)
    } catch {
      case nfe: NumberFormatException => scala.util.Left(WasabiError.ApplicationError(new ApplicationException(s"${text} is not a valid integer.", nfe)))
      case ex: Exception => scala.util.Left(WasabiError.ApplicationError(new ApplicationException(s"${text} is not a valid integer.", ex)))
    }
  }
}
