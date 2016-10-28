package com.intuit.wasabi.data.conf.guice

import com.google.inject.{Inject, Provider}
import com.intuit.wasabi.data.util.Utility
import com.intuit.wasabi.data.util.Constants._
import com.typesafe.config.Config
import org.apache.spark.{SparkConf, SparkContext}

/**
  *     Reads spark configurations for the specific application and then create SparkConfig and
  *     then SparkContext which is then used across application.
  */
class SparkContextProvider extends Provider[SparkContext] {
  var appConfig: Config = null

  @Inject
  def this(appConfig: Config) {
    this
    this.appConfig=appConfig
  }

  override def get(): SparkContext = {
    val appSparkConfigMap = Utility.configToMap(appConfig.getConfig("spark"))

    val conf = new SparkConf()
    conf.setAll(appSparkConfigMap)

    appConfig.hasPath(APP_ARG_MASTER) match {
      case true => conf.setMaster(appConfig.getString(APP_ARG_MASTER))
      case false => //do nothing this means app is being run on cluster
    }

    new SparkContext(conf)
  }
}
