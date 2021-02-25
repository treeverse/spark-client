package io.treeverse.examples

import io.treeverse.clients.LakeFSContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkConf

class ConfFromEnv(private val kvs: Seq[(String, String)]) {
  def this() = this(Seq.empty)

  def build(): SparkConf = new SparkConf().setAll(kvs)

  def env(name: String, envName: String) =
    if (sys.env.contains(envName)) new ConfFromEnv(kvs :+ name -> sys.env(envName)) else this

  def envOr(name: String, envName: String, default: String) =
    new ConfFromEnv(kvs :+ name -> sys.env.getOrElse(envName, default))
}

object List extends App {
  private def dirs(path: String): Seq[String] =
    path.split("/")
      .dropRight(1)
      .scanLeft("")((a, b: String) => (if (a.isEmpty) "" else a + "/") + b)

  override def main(args: Array[String]) {
    if (args.length != 3) {
      Console.err.println("Usage: ... <repo_name> <commit_id> s3://path/to/output/du")
      System.exit(1)
    }
    val conf = new ConfFromEnv()
      .envOr ("spark.hadoop.lakefs.api.url", "LAKEFS_API_URL", "http://localhost:8000/api/v1/")
      .env ("spark.hadoop.lakefs.api.access_key", "LAKEFS_ACCESS_KEY_ID")
      .env ("spark.hadoop.lakefs.api.secret_key", "LAKEFS_SECRET_ACCESS_KEY")
      .build()

    val spark = SparkSession.builder().appName("I can list").config(conf).getOrCreate()

    val sc = spark.sparkContext
    val repo = args(0)
    val ref = args(1)
    val outputPath = args(2)
    val files = LakeFSContext.newRDD(sc, repo, ref)

    val size = files.flatMap({ case (key, entry) => dirs(new String(key)).map(d => (d, entry.message.getSize())) })
      .reduceByKey(_ + _)

    size.saveAsTextFile(outputPath)

    sc.stop()
  }
}
