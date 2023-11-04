package graph_utils

import org.slf4j.LoggerFactory
/*
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
 */
import java.nio.file._
import play.api.libs.json._
import play.api.libs.functional.syntax._

import aws_utils.AWSUtils.getS3File

/**
 * This is the class whose object is stored in the JSON file.
 * When the JSON graph file is loaded back an
 * object of this class is instantiated.
 * @param adjList : The adjacecny list of the graph
 * @param nodeParameters : Stored value and valuable data bool for each node
 */
case class SerializedGraph(adjList: Map[Int, List[(Int)]], nodeParameters: Map[Int, (Double, Boolean)])

/**
 * The LoadGraph object is used to read the graph
 * in JSON file and instantiate it as an object
 * of SerializedGraph class.
 */
object LoadGraph {
  val logger = LoggerFactory.getLogger(getClass)

  implicit val tupleReads: Reads[(Double, Boolean)] = Reads { json =>
    for {
      d <- (json(0)).validate[Double]
      b <- (json(1)).validate[Boolean]
    } yield (d, b)
  }

  implicit val tupleWrites: Writes[(Double, Boolean)] = Writes { tuple =>
    Json.arr(tuple._1, tuple._2)
  }

  implicit val serializedGraphFormat: Format[SerializedGraph] = Json.format[SerializedGraph]


  def loadGraph(path: String): SerializedGraph = {
    val jsonStr = if (path.toLowerCase.contains("s3")) getS3File(path) else new String(Files.readAllBytes(Paths.get(path)))

    Json.parse(jsonStr).validate[SerializedGraph] match {
      case JsSuccess(graph, _) =>
        logger.info(s"Loaded graph with ${graph.nodeParameters.size} nodes.")
        graph
      case JsError(errors) =>
        throw new RuntimeException(s"Failed to decode JSON: ${errors.mkString(", ")}")
    }
  }
}
