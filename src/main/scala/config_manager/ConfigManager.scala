package config_manager

import com.typesafe.config.{Config, ConfigFactory}

object ConfigManager {

  private var config: Config = ConfigFactory.load()

  def getConfig: Config = config

  def overrideWithArgs(args: Array[String]): Unit = {
    if (args.length == 3) {
      val overrides = ConfigFactory.parseString(
        s"""
           |locations.originalGraph="${args(0)}"
           |locations.perturbedGraph="${args(1)}"
           |locations.analysisOutputDir="${args(2)}"
           |""".stripMargin)
      config = overrides.withFallback(config)
    }
  }

}