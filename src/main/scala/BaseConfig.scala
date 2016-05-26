package com.ngs.xad.sim.cfg

import scala.util.{ Try, Success, Failure }
import com.typesafe.config.{ Config, ConfigFactory }
import com.typesafe.config.ConfigException.{ Missing, WrongType }
import collection.JavaConversions._
import com.typesafe.scalalogging.StrictLogging

object BaseConfig extends StrictLogging {
  logger.info("Loading config file ...")
  val config: Config = ConfigFactory.load()
}


trait BaseConfig {

  def config = BaseConfig.config

  def getCfgStrListOrElse(skey: String, alt: List[String]): List[String] = {
    Try(config.getStringList(skey)) match {
      case Success(s) => s.toList
      case Failure(e) => alt
    }
  }

  def getCfgIntOrElse(skey: String, idef: Int): Int = {
    Try(config.getInt(skey)) match {
      case Failure(_) => idef
      case Success(ival: Int) => ival
    }
  }

  def getCfgBooleanOrElse(skey: String, bdef: Boolean): Boolean = {
    Try(config.getBoolean(skey)) match {
      case Failure(_) => bdef
      case Success(bval: Boolean) => bval
    }
  }

  def getCfgStringOrElse(key: String, alt: String): String = {
    Try(config.getString(key)) match {
      case Failure(_) => alt
      case Success(sval: String) => sval
    }
  }
}

trait AppConfig extends BaseConfig {
  val appName = getCfgStringOrElse("application.app-name", "Compare App")
  val appLogging = getCfgBooleanOrElse("application.app-logging", false)
  val appTimeout = math.max(20000, math.min(getCfgIntOrElse("application.app-timeout", 1000*60*10), 999999))
  val appMinWord = math.max(3, math.min(getCfgIntOrElse("application.word-min", 3), 10))
  val appMaxWord = math.max(appMinWord, math.min(getCfgIntOrElse("application.word-max", 11), 50))
  val appMinText = math.max(5, math.min(getCfgIntOrElse("application.text-min", 5), 10))
  val appMaxText = math.max(appMinText, math.min(getCfgIntOrElse("application.text-max", 1000), 1500))
  val appTokOcurrence = math.max(50, math.min(getCfgIntOrElse("application.token-occurrence", 20000), 900000))
  val appTokenNever = getCfgBooleanOrElse("application.token-never", false)
}

trait AkkaConfig extends BaseConfig {
  val processors: Int = Runtime.getRuntime.availableProcessors
  val maxActors = math.max(1, getCfgIntOrElse("akka.max-actors", 10))
  val maxQueueSize = math.max(1, getCfgIntOrElse("akka.max-queue-size", 3))
  val actorLogging = getCfgBooleanOrElse("akka.actor-logging", false)
}

trait AllConfigs extends BaseConfig with AppConfig with AkkaConfig {
  def printAll(): Unit = {
    println(s"appName: ${appName.toString}")
    println(s"appLogging: ${appLogging.toString}")
    println(s"processors: ${processors.toString}")
    println(s"maxActors: ${maxActors.toString}")
    println(s"maxQueSize: ${maxQueueSize.toString}")
    println(s"actorVerbose: ${actorLogging.toString}")
  }
}
