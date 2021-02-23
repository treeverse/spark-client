package io.treeverse.examples

import io.treeverse.clients.LakeFSContext
import org.apache.spark.sql.SparkSession

object List extends App {
  private def dirs(path: String): Seq[String] =
    path.split("/")
      .dropRight(1)
      .scanLeft("")((a, b: String) => a + "/" + b)

  override def main(args: Array[String]) {
    if (args.length != 2) {
      Console.err.println("Usage: ... <repo_name> <commit_id>")
      System.exit(1)
    }
    val spark = SparkSession.builder().appName("I can list")
      .config("spark.hadoop.lakefs.api.url", "http://localhost:8000/api/v1/")
      .config("spark.hadoop.lakefs.api.access_key", "...")
      .config("spark.hadoop.lakefs.api.secret_key", "...")
      .getOrCreate()
    val sc = spark.sparkContext
    val repo = args(0)
    val ref = args(1)
    val files = LakeFSContext.newRDD(sc, repo, ref)

    val size = files.flatMapValues(entry => dirs(entry.message.getAddress).map(d => (d, entry.message.getSize())))
      .map({ case (_, ds) => ds })
      .reduceByKey(_ + _)


    size.top(100000)
      .collect({ case (path, size) => Console.printf("%s\t%d\n", path, size) })

    sc.stop()
  }
}
