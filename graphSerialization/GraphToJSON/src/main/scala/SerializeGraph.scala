import NetGraphAlgebraDefs.NodeObject

import scala.util.Using
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import org.slf4j.LoggerFactory

import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.*

case class SerializedGraph(adjList: Map[Int, List[(Int)]], nodeParameters: Map[Int, (Double, Boolean)])

object SerializeGraph {
  val logger = LoggerFactory.getLogger(getClass)

  def saveGraph(g: NetGraphAlgebraDefs.NetGraph, outputPath: String): Unit = {

    val allNodes = g.sm.nodes.asScala.toList
    val edgeList = g.sm.edges.asScala.toList.map { element =>
      element.asScala.toList
    }
    val adjacencyList: Map[Int, List[Int]] = allNodes.map { node =>
      val allSrcs = edgeList.collect {
        case innerList if innerList(0).id == node.id => innerList(1).id
      }
      node.id -> allSrcs
    }.toMap

    def findNodeById(idToFind: Int): NodeObject= {
      val matchingNode = allNodes.find(_.id == idToFind)
      matchingNode match {
        case Some(node) => node
        case None => throw new NoSuchElementException(s"Custom exception no node found with id $idToFind")
      }
    }

    val nodeParameters = adjacencyList.map { case (key, value) =>
      key -> (findNodeById(key).storedValue, findNodeById(key).valuableData)
    }

    val sg = SerializedGraph(adjacencyList, nodeParameters)
    val jsonString = sg.asJson.noSpaces

    val filePath = s"$outputPath"
    if (!filePath.contains("s3")) {
      Using(Files.newBufferedWriter(Paths.get(filePath))) { writer =>
        writer.write(jsonString)
      }.getOrElse(println(s"Failed to write JSON to file: $filePath"))
    } else {
    }

    logger.info(s"Graph saved: $outputPath")

  }

}
