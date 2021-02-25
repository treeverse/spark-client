package io.treeverse.clients

import com.google.common.cache.CacheBuilder
import org.json4s._
import org.json4s.native.JsonMethods._
import scalaj.http.Http

import java.net.URI
import java.time.Duration

class ApiClient(apiUrl: String, accessKey: String, secretKey: String) {
  private val storageNamespaceCache = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMinutes(2)).build[String, String]()

  private def getStorageNamespace(repoName: String): String = {
    storageNamespaceCache.get(repoName, () => {
      val getRepositoryURI = URI.create("%s/repositories/%s".format(apiUrl, repoName)).normalize()
      val resp = Http(getRepositoryURI.toString).header("Accept", "application/json").auth(accessKey, secretKey).asString
      val JString(storageNamespace) = parse(resp.body) \ "storage_namespace"
      var snUri = URI.create(storageNamespace)
      snUri = new URI(if (snUri.getScheme == "s3") "s3a" else snUri.getScheme, snUri.getHost, snUri.getPath, snUri.getFragment)
      snUri.normalize().toString
    })
  }

  def getMetaRangeURL(repoName: String, commitID: String): String = {
    val getCommitURI = URI.create("%s/repositories/%s/commits/%s".format(apiUrl, repoName, commitID)).normalize()
    val commitResp = Http(getCommitURI.toString).header("Accept", "application/json").auth(accessKey, secretKey).asString
    val commit = parse(commitResp.body)
    val metaRangeID = commit \ "meta_range_id" match {
      case JString(metaRangeID) => metaRangeID
      case _ => // TODO(ariels): Bad parse exception type
        throw new RuntimeException(s"expected string meta_range_id in ${commitResp.body}")
    }

    val getMetaRangeURI = URI.create("%s/repositories/%s/metadata/meta_range/%s".format(apiUrl, repoName, metaRangeID)).normalize()
    val metaRangeResp = Http(getMetaRangeURI.toString).header("Accept", "application/json").auth(accessKey, secretKey).asString
    val location = metaRangeResp.header("Location").get
    URI.create(getStorageNamespace(repoName) + "/" + location).normalize().toString
  }

  def getRangeURL(repoName: String, rangeID: String): String = {
    val getRangeURI = URI.create("%s/repositories/%s/metadata/range/%s".format(apiUrl, repoName, rangeID)).normalize()
    val resp = Http(getRangeURI.toString).header("Accept", "application/json").auth(accessKey, secretKey).asString
    URI.create(getStorageNamespace(repoName) + "/" + resp.header("Location").get).normalize().toString
  }
}
