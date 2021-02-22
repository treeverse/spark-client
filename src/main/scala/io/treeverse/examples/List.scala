package io.treeverse.clients.example

import org.apache.spark.sql.SparkSession
import io.treeverse.clients.{LakeFSContext, LakeFSInputFormat, WithIdentifier}
import io.treeverse.catalog.Catalog
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object List extends App {
  private def dirs(path: String): Seq[String] =
    path.split("/")
      .dropRight(1)
      .scanLeft("")((a, b: String) => a + "/" + b)

  override def main(args: Array[String]) {
    val spark = SparkSession.builder().master("local").appName("I can list")
      .config("spark.hadoop.lakefs.api.url", "http://localhost:8000/api/v1/")
      .config("spark.hadoop.lakefs.api.access_key", "AKIAIOSFODNN7EXAMPLE")
      .config("spark.hadoop.lakefs.api.secret_key", "walrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY")
      .getOrCreate()
    val sc = spark.sparkContext
    val files = LakeFSContext.newRDD(sc, "example-repo", "c7be1745d24c72020001b4954fc33661652698b07a357832ff56137f68f63b0f")
    val count = files.flatMapValues(entry => dirs(entry.message.getAddress))
      .map({ case (_, s) => s })
      .countByValue

    count.collect({ case (path, count) => Console.printf("%s\t%d\n", path, count) })

    sc.stop()
  }
}
