package com.intuit.wasabi.data.conf.guice

import com.google.inject.{Inject, Provider}
import org.apache.spark.SparkContext
import org.apache.spark.sql.cassandra.CassandraSQLContext

/**
  * Created by nbarge on 10/24/16.
  */
//------ Provider for SQLContext
class CassandraSQLContextProvider extends Provider[CassandraSQLContext] {
  var sc: SparkContext = null

  @Inject
  def this(sc: SparkContext) {
    this
    this.sc=sc
  }

  override def get = new CassandraSQLContext(sc)
}
