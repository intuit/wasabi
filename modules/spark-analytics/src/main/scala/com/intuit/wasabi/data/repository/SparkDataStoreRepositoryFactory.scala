package com.intuit.wasabi.data.repository

import cats.data.Xor
import cats.data.Xor._
import com.intuit.wasabi.data.exception.WasabiError.RepositoryError
import com.intuit.wasabi.data.exception.{RepositoryException, WasabiError}
import com.intuit.wasabi.data.repository.cassandra.SparkCassandraRepository
import com.intuit.wasabi.data.repository.mysql.SparkMySqlRepository
import com.intuit.wasabi.data.util.Constants
import org.apache.spark.{Logging, SparkContext}
import org.slf4j.{Logger, LoggerFactory}

/**
  * Errors:
  */
final case class InvalidInputMissingDataStore() extends RepositoryException("Invalid input, please provide DataStore name..")
final case class InvalidInputDataStoreName(msg: String) extends RepositoryException(msg)

/**
  * Factory to create & return different DataStore's concrete repositories.
  *
  */
object SparkDataStoreRepositoryFactory extends Logging {
  /**
    * Factory method to create & return different DataStore's concrete repositories.
    *
    * @param dataStore DataStore of which repository needs to be created.
    * @param sc SparkContext
    * @param options different options are needed by different DataStores.
    *
    * @return a concrete repository for a given DataStore or WasabiError.Repository error
    *
    */
  def getSparkDataStoreRepository(dataStore: String, sc: SparkContext, options: Map[String, String]) : WasabiError Xor SparkDataStoreRepository = {
    if(log.isDebugEnabled) log.debug(s"getSparkDataStoreRepository() - START - dataStore=$dataStore, options=$options")
    if(dataStore==null) {
      left(WasabiError.RepositoryError(InvalidInputMissingDataStore()))
    }
    var repository : SparkDataStoreRepository = null
    if(dataStore.equalsIgnoreCase(Constants.DATASTORE_CASSANDRA)) {
      //repository = new SparkCassandraRepository(sc, new DataStoreConnectionProperties(options), null)
    } else {
      left(WasabiError.RepositoryError(InvalidInputDataStoreName(s"Invalid input, repository for the given DataStore=$dataStore does not exists..")))
    }

    if(log.isDebugEnabled) log.debug(s"getSparkDataStoreRepository() - FINISHED")
    right(repository)
  }
}
