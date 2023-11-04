package graph_utils

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import java.util.Random

/**
 * This object is used to perform parallel
 * random walks on the graph passed in the arguments.
 */
object ParalelRandomWalks {

  def randomWalks(sc: SparkContext, adjList: Map[Int, List[Int]], totalWalks: Int, maxSteps: Int): (Map[Int, List[Int]], Map[String, Double]) = {
    val vertices = adjList.keys.toArray
    val broadcastedAdjList = sc.broadcast(adjList)

    val walksRDD = sc.parallelize(1 to totalWalks, totalWalks)
    val parallelWalks = walksRDD.map { _ =>
      val rnd = new Random()
      val randomVertex = vertices(rnd.nextInt(vertices.length))
      walk(broadcastedAdjList.value, randomVertex, maxSteps-1)
    }
    (extractAdjacencyList(parallelWalks), walkStats(parallelWalks))
  }

  /**
   * In a walk outgoing edges are picked randomly until
   * a node is reached without any outgoing edges or
   * the maxSteps number of steps are attained.
   * @param adjList : Perturbed graph
   * @param start : random starting node
   * @param maxSteps : max steps in a walk
   * @return : List of nodes in a walk
   */

  def walk(adjList: Map[Int, List[Int]], start: Int, maxSteps: Int): List[Int] = {
    var current = start
    val path = List.newBuilder[Int]
    path += current

    for (_ <- 1 to maxSteps) {
      val neighbors = adjList.getOrElse(current, List())
      if (neighbors.nonEmpty) {
        val rnd = new Random()
        current = neighbors(rnd
          .nextInt(neighbors.length))
        path += current
      } else {
        return path.result()
      }
    }
    path.result()
  }

  /**
   * This function is used to condense the list of walks
   * into a graph adjacecny list.
   * @param walks : List of walks as RDD
   * @return : Graph as adjacecny list
   */
  def extractAdjacencyList(walks: RDD[List[Int]]): Map[Int, List[Int]] = {
    val adjacencyListRDD = walks.flatMap { walk =>
        walk.sliding(2).toList.collect {
          case List(a, b) => (a, b)
        }
      }.groupByKey()
      .mapValues(iter => iter.toSet.toList)

    // Collecting as Map
    adjacencyListRDD.collectAsMap().toMap
  }

  def walkStats(parallelWalks: RDD[List[Int]]): Map[String, Double] = {
    val lengths: RDD[Int] = parallelWalks.map(_.length)

    val minLen = lengths.min()
    val maxLen = lengths.max()
    val meanLen = lengths.mean()

    val sortedLengths = lengths.sortBy(identity).collect()
    val medianLen = if (sortedLengths.length % 2 == 0) {
      (sortedLengths(sortedLengths.length / 2 - 1) + sortedLengths(sortedLengths.length / 2)) / 2.0
    } else {
      sortedLengths(sortedLengths.length / 2).toDouble
    }

    Map(
      "min" -> minLen.toInt,
      "max" -> maxLen.toInt,
      "median" -> medianLen,
      "mean" -> meanLen
    )
  }
}
