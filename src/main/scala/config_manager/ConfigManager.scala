package config_manager

import com.typesafe.config.{Config, ConfigFactory}

/**
 * This class is used to initialize the config file.
 * If inputs are provided through the command line arguments,
 * they will overwrite the parameters in the config file.
 */

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