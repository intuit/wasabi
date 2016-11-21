package com.intuit.wasabi.tests.data.udf

import java.sql.Timestamp
import java.time.{Instant, ZoneId}
import java.util.UUID

import com.intuit.wasabi.data.util.Constants._


class StringToUUIDFunc() extends Function1[String, UUID] with Serializable {
  def apply(text: String): UUID = UUID.fromString(text)
}

class UUIDToStringFunc() extends Function1[UUID, String] with Serializable {
  def apply(uuid: UUID): String = uuid.toString
}

object TestUUID {
  def main(args: Array[String]): Unit = {
    val u1 = UUID.randomUUID().toString
    //val fromTS = org.apache.cassandra.utils.UUIDGen.getTimeUUID(System.currentTimeMillis);
    println(s"u1=$u1")
    //println(s"fromTS=$fromTS")

  }
}