package save_yaml

import aws_utils.AWSUtils.writeS3string
import config_manager.ConfigManager
import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.{DumperOptions, Yaml}

import java.io.FileWriter
import scala.collection.JavaConverters._

object SaveAsYaml {
  private val logger = LoggerFactory.getLogger(SaveAsYaml.getClass)
  private val config = ConfigManager.getConfig
  private val analysisOutputDir = config.getString("locations.analysisOutputDir").stripSuffix("/")

  /**
   * This function takes input a Map and saves it in YAML file.
   * @param data : as Map
   * @param fileName : output file name
   */
  def saveMapAsYaml(data: Map[String, Any], fileName: String): Unit = {

    def scalaToJavaMapConversion(map: Map[String, Any]): java.util.Map[String, Any] = {
      map.map {
        case (k, v: Map[_, _]) => k -> scalaToJavaMapConversion(v.asInstanceOf[Map[String, Any]])
        case (k, v: Double) => k -> math.round(v * 10000) / 10000.0
        case (k, v) => k -> v
      }.asJava
    }

    val javaMap = scalaToJavaMapConversion(data)

    val options = new DumperOptions()
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
    options.setPrettyFlow(true)
    val yaml = new Yaml(options)
    val path = s"$analysisOutputDir/$fileName"
    if (!path.toLowerCase.contains("s3")){
      val writer = new FileWriter(path)
      try {
        yaml.dump(javaMap, writer)
      } finally {
        writer.close()
      }
    }else{
        val yamlString = yaml.dump(javaMap)
        writeS3string(yamlString, path)
    }

  }
}
