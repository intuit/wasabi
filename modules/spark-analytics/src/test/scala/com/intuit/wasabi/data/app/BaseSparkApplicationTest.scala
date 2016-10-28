package com.intuit.wasabi.data.app

import com.google.inject.Guice
import com.intuit.wasabi.data.app.{BaseSparkApplication, MigrateDataApplication}
import com.intuit.wasabi.data.conf.AppConfig
import com.intuit.wasabi.data.conf.guice.migratedata.MockMigrateDataApplicationDI
import org.apache.spark.SparkContext
import org.scalatest.{BeforeAndAfterAll, FunSuite}

/**
  * Created by nbarge on 10/21/16.
  */
class BaseSparkApplicationTest(appId: String) extends FunSuite with BeforeAndAfterAll {

}
