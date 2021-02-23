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
    var resp = Http(getCommitURI.toString).header("Accept", "application/json").auth(accessKey, secretKey).asString
    val JString(metaRangeID) = parse(resp.body) \ "meta_range_id"
    val getMetaRangeURI = URI.create("%s/repositories/%s/metadata/meta_range/%s".format(apiUrl, repoName, metaRangeID)).normalize()
    resp = Http(getMetaRangeURI.toString).header("Accept", "application/json").auth(accessKey, secretKey).asString
    URI.create(getStorageNamespace(repoName) + "/" + resp.header("Location").get).normalize().toString
  }

  def getRangeURL(repoName: String, rangeID: String): String = {
    val getRangeURI = URI.create("%s/repositories/%s/metadata/range/%s".format(apiUrl, repoName, rangeID)).normalize()
    val resp = Http(getRangeURI.toString).header("Accept", "application/json").auth(accessKey, secretKey).asString
    URI.create(getStorageNamespace(repoName) + "/" + resp.header("Location").get).normalize().toString
  }
}
