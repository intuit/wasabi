package com.intuit.wasabi.data.conf

import java.net.InetAddress

import com.intuit.wasabi.data.util.Utility
import com.intuit.wasabi.data.util.Constants._
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.Logging
import org.slf4j.LoggerFactory

import scala.util.Try

/**
  * Application settings. First attempts to acquire from the deploy environment.
  * If not exists, then from -D java system properties, else a default config.
  *
  * Any of these can also be overridden by your own application.conf.
  *
  */

class AppConfig(conf: Option[Config] = None) extends Serializable with Logging {

  val localAddress = InetAddress.getLocalHost.getHostAddress
  val util = Utility
  val rootConfig: Config = conf match {
    case Some(c) => c.withFallback(ConfigFactory.load())
    case _ => ConfigFactory.load
  }

  val defaultConfig = rootConfig.getConfig("default").withFallback(rootConfig)
  val perfLog = LoggerFactory.getLogger(PERFORMANCE_LOGGER)

  /** attempts to acquire from environment, then java system properties */
  def withFallback[T](env: Try[T], key: String): Option[T] = env match {
    case null => None // scalastyle:ignore
    case value => value.toOption
  }

  /**
    *
    * @param appId of a spark application which configurations are required.
    *
    * @return configurations of a spark application with fallback to default application configurations
    *
    */
  def getConfigForApp(appId: String) : Config = {
    val config = rootConfig.getConfig(appId).withFallback(defaultConfig)
    config
  }
}
