import NetGraphAlgebraDefs.*
import org.slf4j.LoggerFactory

import java.io.{FileInputStream, ObjectInputStream}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

object LoadNetGameSimGraph {
  val logger = LoggerFactory.getLogger(getClass)

  def loadGraph(path: String): Option[NetGraph] = {
    Try {
      val inputStream = new FileInputStream(path)
      val ois = new ObjectInputStream(inputStream)

      val ng = ois.readObject().asInstanceOf[List[NetGraphComponent]]

      ois.close()
      inputStream.close()

      ng
    } match {
      case Success(listOfNetComponents) =>
        val nodes = listOfNetComponents.collect { case node: NodeObject => node }
        val edges = listOfNetComponents.collect { case edge: Action => edge }

        NetModelAlgebra(nodes, edges)

      case Failure(exception) =>
        logger.error(s"An error occurred: ${exception.getMessage}")
        exception.printStackTrace()
        None
    }
  }

}
