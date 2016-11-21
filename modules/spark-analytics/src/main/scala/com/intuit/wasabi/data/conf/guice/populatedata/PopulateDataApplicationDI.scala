package com.intuit.wasabi.data.conf.guice.populatedata

import com.intuit.wasabi.data.conf.guice.DefaultCassandraRepositoryDI
import com.typesafe.config.Config
import org.apache.spark.SparkContext

class PopulateDataApplicationDI(appConfig: Config, sc: Option[SparkContext] = None) extends DefaultCassandraRepositoryDI(appConfig, sc)
