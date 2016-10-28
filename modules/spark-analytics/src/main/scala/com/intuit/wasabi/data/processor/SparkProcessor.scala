package com.intuit.wasabi.data.processor

import cats.data.Xor
import com.intuit.wasabi.data.conf.AppConfig
import com.intuit.wasabi.data.exception.WasabiError
import com.intuit.wasabi.data.exception.WasabiError.ProcessorError
import com.intuit.wasabi.data.repository.SparkDataStoreRepositoryFactory
import org.apache.spark.SparkContext
import org.apache.spark.sql.SaveMode

/**
  * SPARK processor is a single configurable functionally complete unit of work which can be used/plugged into
  * different spark applications.
  */
trait SparkProcessor extends AppConfig {

  /**
    * This single process method is used to invoke Spark Processor.
    */
  def process(processorID: String): WasabiError Xor Unit
}
