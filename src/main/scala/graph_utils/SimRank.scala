package graph_utils

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.broadcast.Broadcast
import scala.collection.mutable

object SimRank {
  /**
   * This function calculates SimRank scores for each
   * combination of valuable node in original and perturbed graph.
   * @param sc : Spark Context
   * @param originalGraph : adjacecny list of original graph
   * @param perturbedGraph : adjacecny list of traversed perturbed graph
   * @param originalNodes : stored value and valuable data bool for each node of original graph
   * @param perturbedNodes : stored value and valuable data bool for each node of perturbed graph
   * @param maxIterations : max iterations of SimRank
   * @param C : Decay factor
   * @return : Map of original_node -> (perturbed_node, SimScore)
   */
  def calculateSimRank(
                        sc: SparkContext,
                        originalGraph: Map[Int, List[Int]],
                        perturbedGraph: Map[Int, List[Int]],
                        originalNodes: Map[Int, (Double, Boolean)],
                        perturbedNodes: Map[Int, (Double, Boolean)],
                        maxIterations: Int = 3,
                        C: Double = 0.9
                      ): Map[Int, (Int, Double)] = {

    val originalGraphBroadcast: Broadcast[Map[Int, List[Int]]] = sc.broadcast(originalGraph)
    val perturbedGraphBroadcast: Broadcast[Map[Int, List[Int]]] = sc.broadcast(perturbedGraph)
    val originalNodesBroadcast: Broadcast[Map[Int, (Double, Boolean)]] = sc.broadcast(originalNodes)
    val perturbedNodesBroadcast: Broadcast[Map[Int, (Double, Boolean)]] = sc.broadcast(perturbedNodes)

    val memo = mutable.Map[(Int, Int), Double]()

    def simRank(node1: Int, node2: Int, iteration: Int): Double = {
      if (iteration >= maxIterations) return 0.0

      memo.get((node1, node2)) match {
        case Some(similarity) => return similarity
        case None =>
      }

      val perStoredValue = perturbedNodesBroadcast.value(node1)._1
      val orgStoredValue = originalNodesBroadcast.value(node2)._1

      val similarity = if (perStoredValue == orgStoredValue) {
        1.0
      } else {
        val neighbors1 = perturbedGraphBroadcast.value.getOrElse(node1, List())
        val neighbors2 = originalGraphBroadcast.value.getOrElse(node2, List())

        if (neighbors1.isEmpty || neighbors2.isEmpty) {
          0.0
        } else {
          val similarities = for {
            n1 <- neighbors1
            n2 <- neighbors2
          } yield simRank(n1, n2, iteration + 1)

          C * similarities.sum / (neighbors1.size * neighbors2.size)
        }
      }

      memo += ((node1, node2) -> similarity)
      similarity
    }

    //val valuableNodesOriginal = originalNodes.filter(_._2._2).keys
    val valuableNodesOriginal = originalNodes.filterKeys(perturbedNodes.contains).filter(_._2._2).keys
    val valuableNodesPerturbed = perturbedNodes.filter(_._2._2).keys
    val cartesianProduct = valuableNodesOriginal.flatMap(n1 => valuableNodesPerturbed.map(n2 => (n1, n2)))
    val results: RDD[(Int, (Int, Double))] = sc.parallelize(cartesianProduct.toList)
      .map { case (node1, node2) => (node1, (node2, simRank(node2, node1, iteration = 0))) }

    val maxSimilarities = results.reduceByKey((a, b) => if (a._2 > b._2) a else b)

    maxSimilarities.collect().toMap
  }
}
