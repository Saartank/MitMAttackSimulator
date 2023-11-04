import org.slf4j.LoggerFactory
import config_manager.ConfigManager
import org.apache.spark.{SparkConf, SparkContext}
import graph_utils.{LoadGraph, ParalelRandomWalks, SimRank}
import analyze.Analyze.analyze
import save_yaml.SaveAsYaml.saveMapAsYaml
object Main {
  def main(args: Array[String]): Unit = {
    ConfigManager.overrideWithArgs(args)
    val config = ConfigManager.getConfig
    val logger = LoggerFactory.getLogger(getClass)

    val originalGraph = config.getString("locations.originalGraph")
    val perturbedGraph = config.getString("locations.perturbedGraph")
    val analysisOutputDir = config.getString("locations.analysisOutputDir")

    val numberOfParallelWalks = config.getInt("parameters.numberOfParallelWalks")
    val maxStepsPerWalk = config.getInt("parameters.maxStepsPerWalk")
    val simRankDecayFactor = config.getDouble("parameters.simRankDecayFactor")
    val simRankIterations = config.getInt("parameters.simRankIterations")
    val attackThreshold = config.getDouble("parameters.attackThreshold")

    logger.info(s"Original Graph Path: $originalGraph")
    logger.info(s"Perturbed Graph Path: $perturbedGraph")
    logger.info(s"Analysis Output Directory: $analysisOutputDir")
    logger.info(s"Number Of Parallel Walks: $numberOfParallelWalks")
    logger.info(s"Max Steps Per Walk: $maxStepsPerWalk")
    logger.info(s"Sim Rank Decay Factor: $simRankDecayFactor")
    logger.info(s"Sim Rank Iterations: $simRankIterations")
    logger.info(s"Attack Threshold: $attackThreshold")

    val og = LoadGraph.loadGraph(originalGraph)
    val pg = LoadGraph.loadGraph(perturbedGraph)

    val conf = new SparkConf().setAppName("RandomWalksOnGraph")
    if (!analysisOutputDir.toLowerCase.contains("s3")) {
      conf.setMaster("local[*]")
    }
    val sc = new SparkContext(conf)

    val (exploredPerturbedGraph, walkStats) = ParalelRandomWalks.randomWalks(sc, pg.adjList, numberOfParallelWalks, maxStepsPerWalk)

    val simScores = SimRank.calculateSimRank(sc, og.adjList, exploredPerturbedGraph, og.nodeParameters, pg.nodeParameters, simRankIterations, simRankDecayFactor)

    // Restructuring function output to make it readable and store in YAML
    val modifiedSimScores: Map[String, Map[String, Any]] = simScores.map {
      case (originalValuableNodeID, (perturbedValuableNodeID, simScore)) =>
        originalValuableNodeID.toString -> Map(
          "perturbedValuableNodeID" -> perturbedValuableNodeID.toString,
          "SimScore" -> simScore
        )
    }

    saveMapAsYaml(modifiedSimScores, "simScores.yaml")
    saveMapAsYaml(analyze(og.nodeParameters, pg.nodeParameters, simScores, attackThreshold,exploredPerturbedGraph, walkStats), "programOutput.yaml")

    logger.info(s"Congratulations! Job Completed! Please find the program output in: $analysisOutputDir")

  }
}