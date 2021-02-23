package io.treeverse.clients

import io.treeverse.catalog.Catalog
import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapred.InvalidJobConfException
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession

object LakeFSContext {
  val LAKEFS_CONF_API_URL_KEY = "lakefs.api.url"
  val LAKEFS_CONF_API_ACCESS_KEY_KEY = "lakefs.api.access_key"
  val LAKEFS_CONF_API_SECRET_KEY_KEY = "lakefs.api.secret_key"
  val LAKEFS_CONF_JOB_REPO_NAME_KEY = "lakefs.job.repo_name"
  val LAKEFS_CONF_JOB_COMMIT_ID_KEY = "lakefs.job.commit_id"

  def newRDD(
      sc: SparkContext,
      repoName: String,
      commitID: String
  ): RDD[(Array[Byte], WithIdentifier[Catalog.Entry])] = {
    val conf = new Configuration(sc.hadoopConfiguration)
    conf.set(LAKEFS_CONF_JOB_REPO_NAME_KEY, repoName)
    conf.set(LAKEFS_CONF_JOB_COMMIT_ID_KEY, commitID)
    if (StringUtils.isBlank(conf.get(LAKEFS_CONF_API_URL_KEY))) {
      throw new InvalidJobConfException(
        "%s must not be empty".format(LAKEFS_CONF_API_URL_KEY)
      )
    }
    if (StringUtils.isBlank(conf.get(LAKEFS_CONF_API_ACCESS_KEY_KEY))) {
      throw new InvalidJobConfException(
        "%s must not be empty".format(LAKEFS_CONF_API_ACCESS_KEY_KEY)
      )
    }
    if (StringUtils.isBlank(conf.get(LAKEFS_CONF_API_SECRET_KEY_KEY))) {
      throw new InvalidJobConfException(
        "%s must not be empty".format(LAKEFS_CONF_API_SECRET_KEY_KEY)
      )
    }
    sc.newAPIHadoopRDD(
      conf,
      classOf[LakeFSInputFormat],
      classOf[Array[Byte]],
      classOf[WithIdentifier[Catalog.Entry]]
    )
  }
}
