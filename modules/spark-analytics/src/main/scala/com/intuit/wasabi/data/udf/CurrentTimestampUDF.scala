package com.intuit.wasabi.data.udf

import java.sql.Timestamp
import java.time.{Instant, ZoneId}
import com.intuit.wasabi.data.util.Constants._


class CurrentTimestampFunc extends Function1[String, Timestamp] with Serializable {
  def apply(iZoneId: String): Timestamp = {
    var zone: ZoneId = null
    if(iZoneId==null || iZoneId.isEmpty) {
      zone = DEFAULT_ZONE
    } else {
      zone = ZoneId.of(iZoneId)
    }

    //Current machine timestamp in ZONE time
    val currentZonedDateTime = Instant.now().atZone(zone)

    new Timestamp(currentZonedDateTime.toInstant.toEpochMilli)
  }
}