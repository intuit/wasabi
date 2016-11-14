package com.intuit.wasabi.data.conf.guice.populatedata

import com.intuit.wasabi.data.conf.guice.DefaultCassandraRepositoryDI
import com.typesafe.config.Config

class PopulateDataApplicationDI(appConfig: Config) extends DefaultCassandraRepositoryDI(appConfig)
