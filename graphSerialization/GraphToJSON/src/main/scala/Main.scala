import org.slf4j.LoggerFactory

object Main{
  def main(args: Array[String]): Unit = {
    val logger = LoggerFactory.getLogger(getClass)
    logger.info(s"Loading graphs: ${args(0)} and ${args(1)}")
    val g = LoadNetGameSimGraph.loadGraph(args(0))
    val gPer = LoadNetGameSimGraph.loadGraph(args(1))
    logger.info(s"Graphs loaded successfully!")
    logger.info(s"Converting them to JSON now.")

    SerializeGraph.saveGraph(g.get, "original_graph.json")
    SerializeGraph.saveGraph(gPer.get, "perturbed_graph.json")
    
  }
}