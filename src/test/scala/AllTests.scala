import analyze.Analyze
import config_manager.ConfigManager
import graph_utils.SimRank.calculateSimRank
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.flatspec.AnyFlatSpec
import org.slf4j.LoggerFactory
import save_yaml.SaveAsYaml.saveMapAsYaml
import java.nio.file.{Files, Paths}

class AllTests extends AnyFlatSpec {
  val logger = LoggerFactory.getLogger(getClass)

  val originalGraph: Map[Int, List[Int]] =
    Map(
      1 -> List(2, 3),
      2 -> List(1, 3, 4),
      3 -> List(1, 2),
      4 -> List(2)
    )

  val perturbedGraph: Map[Int, List[Int]] =
    Map(
      2 -> List(3, 4),
      3 -> List(2),
      4 -> List(2),
      5 -> List(3)
    )

  val originalNodes: Map[Int, (Double, Boolean)] = Map(
    1 -> (0.5, true),
    2 -> (0.6, true),
    3 -> (0.4, false),
    4 -> (0.3, true)
  )

  val perturbedNodes: Map[Int, (Double, Boolean)] = Map(
    2 -> (0.01, true),
    3 -> (0.7, false),
    4 -> (0.3, true),
    5 -> (0.2, true)
  )

  "calculateSimRank" should "calculate sim rank between valuable nodes of the 2 graphs" in {

    val conf = new SparkConf().setAppName("SimRankTest").setMaster("local")
    val sc = new SparkContext(conf)

    val result = calculateSimRank(sc, originalGraph, perturbedGraph, originalNodes, perturbedNodes)
    sc.stop()
    println(result)
    assert(result(4)==(4,1))
  }

  "simulatedHoneypots" should "return valuable nodes from perturbedNodes not present in originalNodes" in {
    val result = Analyze.simulatedHoneypots(perturbedNodes, originalNodes)
    assert(result == Map(5 -> (0.2, true)))
  }

  "removedValuableNodes" should "return valuable nodes from originalNodes not present in perturbedNodes" in {
    val result = Analyze.removedValuableNodes(originalNodes, perturbedNodes)
    assert(result == Map(1 -> (0.5, true)))
  }

  "totalDiscoverableValuableNodes" should "return valuable nodes present in both maps" in {
    val result = Analyze.totalDiscoverableValuableNodes(originalNodes, perturbedNodes)
    assert(result == Map(4 -> (0.3, true), 2 -> (0.6,true)))
  }

  "calculateScores" should "calculate precision and recall correctly" in {
    val (precision, recall) = Analyze.calculateScores(5, 3, 2)
    assert(precision == 5.0 / 8 && recall == 5.0 / 7)
  }

  "saveMapAsYaml" should "create a YAML file with the specified name" in {

    val config = ConfigManager.getConfig
    val analysisOutputDir = config.getString("locations.analysisOutputDir").stripSuffix("/")

    if (Files.exists(Paths.get(analysisOutputDir)) && Files.isDirectory(Paths.get(analysisOutputDir))){
      logger.info(s"Directory '$analysisOutputDir' exists.")
    } else {
      logger.info(s"Directory '$analysisOutputDir' does not exist. Please create the 'analysisOutputDir' mentioned in the config")
    }

    val sampleData: Map[String, Any] = Map(
      "Statistics" -> Map(
        "True Positives (successful attacks)" -> 212,
        "False Positives (failed attacks)" -> 0,
        "False Negatives (un-explored and explored but missed)" -> 11
      ),
      "Scores" -> Map(
        "Precision" -> 1.0,
        "Recall" -> 0.9507
      ),
    )

    val testFileName = "testFile.yaml"

    saveMapAsYaml(sampleData, testFileName)

    assert(Files.exists(Paths.get(s"$analysisOutputDir/$testFileName")))

    Files.deleteIfExists(Paths.get(s"$analysisOutputDir/$testFileName"))

    assert(!Files.exists(Paths.get(s"$analysisOutputDir/$testFileName")))

  }
}
