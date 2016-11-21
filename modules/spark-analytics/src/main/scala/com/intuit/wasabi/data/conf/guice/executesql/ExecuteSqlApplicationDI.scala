package com.intuit.wasabi.data.conf.guice.executesql

import com.intuit.wasabi.data.conf.guice.DefaultCassandraRepositoryDI
import com.typesafe.config.Config
import org.apache.spark.SparkContext


class ExecuteSqlApplicationDI(appConfig: Config, sc: Option[SparkContext] = None) extends DefaultCassandraRepositoryDI(appConfig, sc)

