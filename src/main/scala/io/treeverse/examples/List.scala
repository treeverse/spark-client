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
    val spark = SparkSession.builder().master("local").appName("I can list")
      .config("spark.hadoop.lakefs.api.url", "http://localhost:8000/api/v1/")
      .config("spark.hadoop.lakefs.api.access_key", "AKIAIOSFODNN7EXAMPLE")
      .config("spark.hadoop.lakefs.api.secret_key", "walrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
      .getOrCreate()
    val sc = spark.sparkContext
    val files = LakeFSContext.newRDD(sc, args(0), args(1))
    val count = files.flatMapValues(entry => dirs(entry.message.getAddress))
      .map({ case (_, s) => s })
      .countByValue

    count.collect({ case (path, count) => Console.printf("%s\t%d\n", path, count) })

    sc.stop()
  }
}
