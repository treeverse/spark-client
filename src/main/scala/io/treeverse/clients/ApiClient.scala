package io.treeverse.clients

import org.json4s._
import org.json4s.native.JsonMethods._
import scalaj.http.Http

class ApiClient(apiUrl : String, accessKey : String, secretKey : String) {
  def getRepositoryStorageNamespace(repoName: String): String = {
    val resp = Http("%s/repositories/%s".format(apiUrl, repoName)).header("Accept", "application/json").auth(accessKey, secretKey).asString
    val JString(storageNamespace) = parse(resp.body) \ "storage_namespace"
    storageNamespace.replace("s3://", "s3a://")
  }

  def getMetaRangeURL(repoName: String, storageNamespace: String, commitID: String): String = {
    var resp = Http("%s/repositories/%s/commits/%s".format(apiUrl, repoName, commitID)).header("Accept", "application/json").auth(accessKey, secretKey).asString
    val JString(metaRangeID) = parse(resp.body) \ "meta_range_id"
    resp = Http("%s/repositories/%s/metadata/meta_range/%s".format(apiUrl, repoName, metaRangeID)).header("Accept", "application/json").auth(accessKey, secretKey).asString
    storageNamespace + "/" + resp.header("Location").get
  }

  def getRangeURL(repoName: String, rangeID: String): String = {
    val resp = Http("%s/repositories/%s/metadata/range/%s".format(apiUrl, repoName, rangeID)).header("Accept", "application/json").auth(accessKey, secretKey).asString
    resp.header("Location").get
  }
}