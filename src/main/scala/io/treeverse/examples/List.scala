package io.treeverse.clients.example

import org.apache.spark.sql.SparkSession
import io.treeverse.clients.WithIdentifier
import io.treeverse.catalog.Catalog
import io.treeverse.clients.LakeFSInputFormat
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object List extends App {
  private def dirs(path: String): Seq[String] =
    path.split("/")
      .dropRight(1)
      .scanLeft("")((a, b: String) => a + "/" + b)

  override def main(args: Array[String]) {
    if (args.length != 1) {
      Console.err.println("Usage: ... s3://path/to/metarange")
      System.exit(1)
    }

    val path = args(0)

    val sc = new SparkContext(new SparkConf().setMaster("local").setAppName("I can list"))

    val files = sc.newAPIHadoopFile[Array[Byte], WithIdentifier[Catalog.Entry], LakeFSInputFormat](path)

    val count = files.flatMapValues(entry => dirs(entry.message.getAddress))
      .map({ case (_, s) => s })
      .countByValue

    count.collect({ case (path, count) => Console.printf("%s\t%d\n", path, count) })

    sc.stop()
  }
}
