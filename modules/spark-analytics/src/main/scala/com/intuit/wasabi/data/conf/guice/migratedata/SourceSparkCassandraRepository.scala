package com.intuit.wasabi.data.conf.guice.migratedata

import com.datastax.spark.connector.cql.CassandraConnector
import com.google.inject.Inject
import com.google.inject.name.Named
import com.intuit.wasabi.data.repository.DataStoreConnectionProperties
import com.intuit.wasabi.data.repository.cassandra.SparkCassandraRepository
import com.intuit.wasabi.data.udf.CurrentTimestampFunc
import org.apache.spark.SparkContext
import org.apache.spark.sql.cassandra.CassandraSQLContext

/**
  * Created by nbarge on 10/24/16.
  */
//------ Provider for source CassandraRepository
class SourceSparkCassandraRepository(sc: SparkContext, options: DataStoreConnectionProperties, connection: CassandraConnector, sqlContext: CassandraSQLContext, currentTimestampFunc: CurrentTimestampFunc) extends SparkCassandraRepository(sc, options, connection, sqlContext, currentTimestampFunc) {
  @Inject
  def this(sc: SparkContext, @Named("SourceDataStoreConnectionProperties") options: DataStoreConnectionProperties, @Named("SourceDataStoreCassandraConnector") connection: CassandraConnector, @Named("SourceDataStoreCassandraSQLContext") sqlContext: CassandraSQLContext, currentTimestampFunc: CurrentTimestampFunc, rType: String = null ) {
    this(sc, options, connection, sqlContext, currentTimestampFunc)
  }
}
