package com.intuit.wasabi.data.conf.guice.executesql

import com.intuit.wasabi.data.conf.guice.DefaultCassandraRepositoryDI
import com.typesafe.config.Config


class ExecuteSqlApplicationDI(appConfig: Config) extends DefaultCassandraRepositoryDI(appConfig)

