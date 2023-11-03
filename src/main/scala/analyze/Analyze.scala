package analyze

object Analyze {

  type NodeMap = Map[Int, (Double, Boolean)]
  type ScoreMap = Map[Int, (Int, Double)]

  def simulatedHoneypots(perturbedNodes: NodeMap, originalNodes: NodeMap): NodeMap = {
    perturbedNodes.filter {
      case (nodeId, (_, isValuable)) => isValuable && !originalNodes.contains(nodeId)
    }
  }

  def modifiedValuableNodes(perturbedNodes: NodeMap, originalNodes: NodeMap): NodeMap = {
    perturbedNodes.filter {
      case (nodeId, (storedValue, isValuable)) =>
        isValuable && originalNodes.get(nodeId).exists(_._1 != storedValue)
    }
  }

  def removedValuableNodes(originalNodes: NodeMap, perturbedNodes: NodeMap): NodeMap = {
    originalNodes.filter {
      case (nodeId, (_, isValuable)) => isValuable && !perturbedNodes.contains(nodeId)
    }
  }

  def totalDiscoverableValuableNodes(originalNodes: NodeMap, perturbedNodes: NodeMap): NodeMap = {
    originalNodes.filter {
      case (nodeId, (_, isValuable)) => isValuable && perturbedNodes.contains(nodeId)
    }
  }

  def successfulAttacks(simScores: ScoreMap, attackThreshold: Double): ScoreMap = {
    simScores.filter {
      case (originalNodeId, (perturbedNodeId, simScore)) =>
        simScore > attackThreshold && originalNodeId == perturbedNodeId
    }
  }

  def failedAttacks(simScores: ScoreMap, attackThreshold: Double): ScoreMap = {
    simScores.filter {
      case (originalNodeId, (perturbedNodeId, simScore)) =>
        simScore > attackThreshold && originalNodeId != perturbedNodeId
    }
  }

  def missedExploredValuableNodes(simScores: ScoreMap, attackThreshold: Double): ScoreMap = {
    simScores.filter {
      case (_, (_, simScore)) => simScore < attackThreshold
    }
  }

  def calculateScores(tp: Int, fp: Int, fn: Int): (Double, Double) = {
    val precision = tp.toDouble / (tp + fp)
    val recall = tp.toDouble / (tp + fn)
    (precision, recall)
  }

  def analyze(originalNodes: NodeMap, perturbedNodes: NodeMap, simScores: ScoreMap, attackThreshold: Double, exploredPerturbedGraph: Map[Int, List[Int]], walkStats: Map[String, Double]): Map[String, Any] = {
    val sh = simulatedHoneypots(perturbedNodes, originalNodes)
    val mvn = modifiedValuableNodes(perturbedNodes, originalNodes)
    val rvn = removedValuableNodes(originalNodes, perturbedNodes)
    val tdvn = totalDiscoverableValuableNodes(originalNodes, perturbedNodes)

    val sa = successfulAttacks(simScores, attackThreshold)
    val fa = failedAttacks(simScores, attackThreshold)
    val mevn = missedExploredValuableNodes(simScores, attackThreshold)

    val (precision, recall) = calculateScores(sa.size, fa.size, tdvn.size - (sa.size + fa.size))

    Map(
      "Simulated Graphs" -> Map(
        "Nodes in Original graph" -> originalNodes.keys.size,
        "Nodes in Perturbed graph" -> perturbedNodes.keys.size
      ),
      "Perturbations" -> Map(
        "Honeypots (Added Valuable Nodes)" -> sh.size,
        "Modified Valuable Nodes" -> mvn.size,
        "Removed Valuable Nodes" -> rvn.size,
        "Total Discoverable Valuable Nodes" -> tdvn.size
      ),
      "Random Walks" -> Map(
        "Distinct Nodes in a Walk" -> walkStats,
        "Percentage of Nodes Covered" -> (exploredPerturbedGraph.keys.size.toDouble * 100) / perturbedNodes.size
      ),
      "Attacks" -> Map(
        "Total Attacks" -> (sa.size + fa.size),
        "Total Successful Attacks" -> sa.size,
        "Valuable Nodes Explored But Missed" -> mevn.size,
        "Attacks on Modified Nodes" -> ((mvn.keys.toSet & sa.keys.toSet).size + (mvn.keys.toSet & fa.keys.toSet).size),
        "Successful Attacks On Modified Nodes" -> (mvn.keys.toSet & sa.keys.toSet).size
      ),
      "Statistics" -> Map(
        "True Positives (successful attacks)" -> sa.size,
        "False Positives (failed attacks)" -> fa.size,
        "False Negatives (un-explored and explored but missed)" -> (tdvn.size - (sa.size + fa.size))
      ),
      "Scores" -> Map(
        "Precision" -> precision,
        "Recall" -> recall
      )
    )
  }
}
