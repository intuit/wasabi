package com.intuit.wasabi.data.conf.guice

import com.google.inject.{AbstractModule, Scopes}
import com.intuit.wasabi.data.udf.CurrentTimestampFunc
import com.typesafe.config.Config
import org.apache.spark.{Logging, SparkContext}

/**
  * Created by nbarge on 10/19/16.
  */

class CommonSparkApplicationDI(appConfig: Config, sc: Option[SparkContext] = None) extends AbstractModule with Logging {
  override def configure(): Unit = {
    bind(classOf[CurrentTimestampFunc]).toInstance(new CurrentTimestampFunc)
    bind(classOf[Config]).toInstance(appConfig)
    sc match {
      case Some(sparkContext) => bind(classOf[SparkContext]).toInstance(sparkContext)
      case _ => bind(classOf[SparkContext]).toProvider(classOf[SparkContextProvider]).in(Scopes.SINGLETON)
    }
  }
}
