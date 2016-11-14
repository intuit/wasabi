package com.intuit.wasabi.data.app

import java.sql.Date
import java.util.UUID

import cats.data.Xor
import cats.data.Xor._
import com.google.inject.Inject
import com.google.inject.name.Named
import com.intuit.wasabi.data.exception.WasabiError
import com.intuit.wasabi.data.repository.SparkDataStoreRepository
import com.typesafe.config.Config
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.{DataFrame, Row, SQLContext, SaveMode}
import org.apache.spark.sql.types._

/**
  * This application is created to execute adhoc SQL statements on DataStore(s). Mainly useful for debugging purpose.
  *
  *
  */
class PopulateDataApplication(sc: SparkContext, appConfig: Config) extends BaseSparkApplication (sc, appConfig)   {

  var repository: SparkDataStoreRepository = null

  @Inject
  def this(sc: SparkContext, appConfig: Config, @Named("DataStoreCassandraRepository") repository: SparkDataStoreRepository, mType: String = null) {
    this(sc, appConfig)
    this.repository=repository
  }

  override def run(): WasabiError Xor Unit = {
    if(log.isInfoEnabled()) log.info("run() - STARTED")
    val sTime = System.currentTimeMillis

    val table = appConfig.getString("table")
    if(log.isInfoEnabled()) log.info(s"table => $table")

    val batchSize = appConfig.getInt("batchSize")
    if(log.isInfoEnabled()) log.info(s"batchSize => $batchSize")

    //Format: num_experiments-number_of_user_assignments_per_experiments-num_batches_or_rdd_partitions
    //Data will be generated per batch/rdd_partition. Each batch size is 100K
    val allocation = appConfig.getString("allocation")
    if(log.isInfoEnabled()) log.info(s"allocation => $allocation")

    var sqlContext0: SQLContext = null
    repository.getSqlContext() match {
      case Left(error: WasabiError) => {}
      case Right(sContext: SQLContext) => sqlContext0=sContext
    }
    val sqlContext = sqlContext0
    import sqlContext.implicits._

    val allocationBands = allocation.split(" ")
    for(allocationBand <- allocationBands) {
      if(log.isInfoEnabled()) log.info(s"allocationBand => $allocationBand")

      val allocBandDetails = allocationBand.split("-")
      val numExperiments = allocBandDetails(0).toInt
      val numUserAssignmentsPerExp = allocBandDetails(1).toInt
      val numBatches=Math.max(numExperiments*numUserAssignmentsPerExp/batchSize, 1)
      val uaPerExpPerPartition=Math.max(numUserAssignmentsPerExp/numBatches, 1)

      if(log.isInfoEnabled()) log.info(s"numExperiments => $numExperiments")
      if(log.isInfoEnabled()) log.info(s"numUserAssignmentsPerExp => $numUserAssignmentsPerExp")
      if(log.isInfoEnabled()) log.info(s"numBatches => $numBatches")
      if(log.isInfoEnabled()) log.info(s"User assignments per experiments per partitions => $uaPerExpPerPartition")

      if(log.isInfoEnabled()) log.info(s"Total user assignments in this band [$allocationBand] => (numExperiments*uaPerExpPerPartition*numBatches) => ${numExperiments*uaPerExpPerPartition*numBatches}")

      val experimentIds: collection.mutable.ArraySeq[String] = new collection.mutable.ArraySeq(numExperiments)
      for(i <- 0 to numExperiments-1) {
        experimentIds(i) = UUID.randomUUID().toString
      }
      if(log.isInfoEnabled()) log.info(s"experimentIds => $experimentIds")

      //val rows: collection.mutable.ArraySeq[UserAssignmentSchema] = new collection.mutable.ArraySeq(numBatches)
      //val createTime = new Date(System.currentTimeMillis())
      //for(i <- 0 to numBatches-1) {
      //  rows(i) = new UserAssignmentSchema(experimentIds(0), s"user_id_1_${i}_0_$i", "PROD", s"bucket_label_1", createTime)
      //}
     // val initExpDF = rows.toDF("experiment_id", "user_id", "context", "bucket_label", "created").repartition(numBatches).rdd
      val initExpDF = sqlContext.emptyDataFrame.repartition(numBatches).rdd
      if(log.isInfoEnabled()) log.info(s"initExpDF.rdd.partitions.size => ${initExpDF.partitions.size}")

      val experimentIdsB = sc.broadcast(experimentIds)
      val numUserAssignmentsPerExpB = sc.broadcast(numUserAssignmentsPerExp)
      val uaPerExpPerPartitionB = sc.broadcast(uaPerExpPerPartition)
      val dataRdd = initExpDF.mapPartitionsWithIndex[UserAssignmentSchema](DataGenerator(numUserAssignmentsPerExpB, experimentIdsB, uaPerExpPerPartitionB).userAssignmentsFunc)

      val allocationBandDF = dataRdd.toDF("experiment_id", "user_id", "context", "bucket_label", "created")
      //allocationBandDF.printSchema()
      allocationBandDF.show(5)

      repository.write(table, SaveMode.Append, allocationBandDF)
    }

    if(perfLog.isInfoEnabled) perfLog.info(s"Time taken by ExecuteSqlApplication[$appId].start() = ${System.currentTimeMillis-sTime} ms")
    if(log.isInfoEnabled()) log.info("run() - FINISHED")

    right()
  }
}

final case class UserAssignmentSchema(experiment_id: String, context: String, user_id: String, bucket_label: String, created: Date)

case class DataGenerator(val uaNumsPerExpB:Broadcast[Int], val experimentIdsB:Broadcast[scala.collection.mutable.ArraySeq[String]], val uaPerExpPerPartitionB: Broadcast[Int]) {
  val userAssignmentsFunc = (partitionId: Int, iRows: Iterator[Row]) => {
    val rows: collection.mutable.ArraySeq[UserAssignmentSchema] = new collection.mutable.ArraySeq(experimentIdsB.value.size*uaPerExpPerPartitionB.value)
    var uaIdx=0
    var expIdx=0
    val bPad = "1234567890123_"
    val uPad = "1234567890123_"

    val createTime = new Date(System.currentTimeMillis())
    for (expId <- experimentIdsB.value) {

      for (i <- 0 to uaPerExpPerPartitionB.value-1) {
        rows(uaIdx) = new UserAssignmentSchema(expId, s"user_id_${uPad}_${expIdx}_${partitionId}_${"%07d".format(i)}", "PROD", s"bucket_label_${bPad}", createTime)
        uaIdx += 1
      }
      expIdx += 1
    }
    rows.iterator
  }
}


