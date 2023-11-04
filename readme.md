# I am
**Name :** Sarthak Agarwal

**UIN :** 679962604

**UIC email :** sagarw35@uic.edu

# Introduction

A "Man-in-the-middle" attack (MitM) occurs when an attacker, who is an insider, deploys a service that may provide useful functionality. This service then observes the communication paths. Based on these paths and the attacker's knowledge of the system, the attacker identifies machines with valuable data and captures them. To prevent such attacks, the organization might introduce modifications to the system by adding honeypots. These are machines that simulate ones with valuable data, and are used to detect an attack if the attacker tries to capture them.

In this homework, we will simulate a MitM attack. The system will be represented as a graph. To generate random graphs, we will use [NetGameSim](https://github.com/0x1DOCD00D/NetGameSim). [NetGameSim](https://github.com/0x1DOCD00D/NetGameSim) produces a random graph and then adds perturbations to it based on controllable parameters. The output from [NetGameSim](https://github.com/0x1DOCD00D/NetGameSim) includes the binary of the original graph, the perturbed graph, and the set of perturbations applied to the original graph. Here, the original graph represents the attacker's knowledge of the actual system. The perturbed graph represents the system altered by the organization to prevent such attacks.

Next, we will conduct random walks on the altered graph representing communication paths. Based on these paths and the attacker's knowledge of the original graph, the attacker's goal will be to correctly identify the nodes with valuable data on the altered graph. This program essentially simulates such an attack and outputs various statistical metrics of the attacks performed. The program is based on Spark to achieve parallelism across multiple computing nodes.

# Program Working

## Directory structure
```

graphSerialization/
├─ GraphToJSON/
│  └─ src/

sampleData/
├─ original_graph.json
├─ outputs/
│  ├─ programOutput.yaml
│  └─ simScores.yaml
└─ perturbed_graph.json

src/
├─ main/
│  ├─ resources/
│  │  └─ application.conf
│  ├─ scala/
│     ├─ Main.scala
│     ├─ analyze/
│     │  └─ Analyze.scala
│     ├─ aws_utils/
│     │  └─ AWSUtils.scala
│     ├─ config_manager/
│     │  └─ ConfigManager.scala
│     ├─ graph_utils/
│     │  ├─ LoadGraph.scala
│     │  ├─ ParalelRandomWalks.scala
│     │  └─ SimRank.scala
│     └─ save_yaml/
│        └─ SaveAsYaml.scala
└─ test/
├─ scala/
└─ AllTests.scala
```
## Graph Serialization

The [NetGameSim](https://github.com/0x1DOCD00D/NetGameSim) is developed using Scala 3 and it outputs graphs in a binary format. However, Spark does not currently support Scala 3. To facilitate the import of these graphs into a Scala 2 project, I serialize the graph into an intermediate JSON format, which can then be read by the primary Scala 2 project. The code for converting the graph to binary is located in the `graphSerialization/` directory.

## Inputs for Primary Scala 2 Project

The main project, located at the root of this repository, utilizes the JSON-formatted graphs produced by `graphSerialization/GraphToJSON`. It requires three inputs:

1. Path to the original graph in JSON format.
2. Path to the perturbed graph in JSON format.
3. Directory path where the program's output will be saved.

Inputs can be provided either through the Command Line Interface (CLI) or via a config file. If provided via the CLI, these values will take precedence over those in the config file.

## Parallel Random Walks and SimRank Using Spark:

To perform parallel random walks in Spark, the perturbed graph is broadcasted. Next, random walks are initiated in parallel from random starting nodes. The number of parallel walks can be specified using the `numberOfParallelWalks` parameter in the config. These walks continue until either a node with no outgoing edges is reached or the maximum number of `maxStepsPerWalk` steps is attained, which can be set in the config file. The paths traversed by these random walks are condensed into a graph. Finally, SimRank is initiated on both the traversed perturbed graph and the original graph. Parallelism is employed again while calculating SimRank. Both the original and traversed graphs are broadcasted, and SimRank is calculated between each combination of valuable nodes present in the perturbed traversed graph and the original graph. For each valuable node, a corresponding node from the perturbed traversed graph with the highest SimScore is chosen. These SimScores are saved in the `simScores.yaml` file within the `analysisOutputDir` directory specified in the config. An attack is simulated if the SimScore between the original valuable node and the corresponding perturbed node is higher than `attackThreshold`. The attack is successful if the node IDs of the two nodes match, otherwise, the attack is considered failed.

## Output Statistics:

The program outputs various statistics related to different parts of the process. These statistics are saved in the `programOutput.yaml` file within the `analysisOutputDir` directory specified in the config. Below is a sample `programOutput.yaml` file:

```
Statistics:
  True Positives (successful attacks): 209
  False Positives (failed attacks): 1
  False Negatives (un-explored and explored but missed): 13
Scores:
  Precision: 0.9952
  Recall: 0.9414
Attacks:
  Total Attacks: 210
  Successful Attacks On Modified Nodes: 1
  Valuable Nodes Explored But Missed: 12
  Total Successful Attacks: 209
  Attacks on Modified Nodes: 2
Perturbations:
  Honeypots (Added Valuable Nodes): 15
  Modified Valuable Nodes: 15
  Removed Valuable Nodes: 20
  Total Discoverable Valuable Nodes: 223
Random Walks:
  Distinct Nodes in a Walk:
    min: 1.0
    max: 100.0
    median: 61.5
    mean: 67.5667
  Percentage of Nodes Covered: 36.0107
Simulated Graphs:
  Nodes in Original graph: 3001
  Nodes in Perturbed graph: 2988
```

## Creating Executable JAR

1. **Clone the Project:**  
   Obtain a copy of the source code on your local machine.

2. **Build the Project:**  
   Navigate to the project directory and execute:
   This command cleans, compiles, and packages the project into an executable JAR file.
```agsl
sbt clean compile assembly
```
3. **Locate the JAR:**  
   Find the generated JAR file inside the `target` folder.

## AWS Deployment

1. **Upload to S3:**
    - Place the necessary files (original graph, perturbed graph) and the JAR into an S3 bucket.
    - Create a folder within the bucket for program output.

2. **Create EMR Job:**
    - Initialize an EMR job equipped with Hadoop and Spark.
    - Ensure compatibility by using emr-6.14.0 which includes Hadoop 3.3.3 and Spark 3.4.1.

3. **Add Custom Step:**
    - Include a custom step in the EMR job configuration, selecting "Spark application" as the type.
    - For the deploy mode, select "client" since the gateway machine is physically co-located with worker machines, although "cluster" mode is also available.
    - Specify the path to the JAR file stored in S3.
    - Provide the S3 paths for the original graph, perturbed graph, and output directory in the arguments section.

# Limitations

In the current approach, there is no communication between parallel random walks i.e. all walks are independent. A better approach would involve the parallel walks exchanging information about the nodes traversed, allowing each walk to attempt exploring new nodes not seen by any of the other parallel walks. This could increase the percentage of explored nodes, which currently stands at approximately 40% using the default parameters. Of course, communicating at each step would be costly; therefore, a middle ground should be sought. This would involve updating the set of all explored nodes only after a certain number of steps have been taken.

# Video Demo 

https://youtu.be/nM44cpEyhxE