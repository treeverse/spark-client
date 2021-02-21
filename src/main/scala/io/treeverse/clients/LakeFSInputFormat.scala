package io.treeverse.clients

import com.google.protobuf.Message
import io.treeverse.catalog.Catalog
import io.treeverse.clients.LakeFSContext._
import io.treeverse.committed.Committed.RangeData
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.Writable
import org.apache.hadoop.mapred.SplitLocationInfo
import org.apache.hadoop.mapreduce._

import java.io.{DataInput, DataOutput, File}
import scala.collection.JavaConverters._

class GravelerSplit(var range: RangeData, var path: Path) extends InputSplit with Writable {
  def this() = this(null, null)

  override def write(out: DataOutput) = {
    val encodedRange = range.toByteArray
    out.writeInt(encodedRange.length)
    out.write(encodedRange)
    val p = path.toString
    out.writeInt(p.length)
    out.writeChars(p)
  }

  override def readFields(in: DataInput) = {
    val encodedRangeLength = in.readInt()
    val encodedRange = new Array[Byte](encodedRangeLength)
    in.readFully(encodedRange)
    range = RangeData.parseFrom(encodedRange)
    val pathLength = in.readInt()
    val p = new StringBuilder
    for (_ <- 1 to pathLength) {
      p += in.readChar()
    }
    path = new Path(p.result)
  }

  override def getLength: Long = range.getEstimatedSize

  override def getLocations: Array[String] = Array.empty[String]

  override def getLocationInfo: Array[SplitLocationInfo] = Array.empty[SplitLocationInfo]
}

class WithIdentifier[Proto <: Message](val id: Array[Byte], val message: Proto) {}

class EntryRecordReader[Proto <: Message](messagePrototype: Proto) extends RecordReader[Array[Byte], WithIdentifier[Proto]] {
  var it: SSTableIterator[Proto] = null
  var item: Item[Proto] = null

  override def initialize(split: InputSplit, context: TaskAttemptContext) = {
    val localFile = File.createTempFile("lakefs", "range")
    localFile.deleteOnExit()
    val gravelerSplit = split.asInstanceOf[GravelerSplit]
    val fs = gravelerSplit.path.getFileSystem(context.getConfiguration)
    fs.copyToLocalFile(gravelerSplit.path, new Path(localFile.getAbsolutePath))
    // TODO(johnnyaug) should we cache this?
    val sstableReader = new SSTableReader(localFile.getAbsolutePath, messagePrototype)
    it = sstableReader.newIterator()
  }

  override def nextKeyValue: Boolean = {
    if (!it.hasNext) {
      return false
    }
    item = it.next
    return true
  }

  override def getCurrentKey = item.key

  override def getCurrentValue = new WithIdentifier(item.id, item.message)

  override def close = it.close

  override def getProgress: Float = {
    0 // TODO(johnnyaug) complete
  }
}

object LakeFSInputFormat {
  private def read[Proto <: Message](reader: SSTableReader[Proto]): Seq[Item[Proto]] =
    reader.newIterator.toSeq
}

class LakeFSInputFormat extends InputFormat[Array[Byte], WithIdentifier[Catalog.Entry]] {
  import LakeFSInputFormat._

  override def getSplits(job: JobContext): java.util.List[InputSplit] = {
    val conf = job.getConfiguration
    val repoName = conf.get(LAKEFS_CONF_JOB_REPO_NAME_KEY)
    val commitID = conf.get(LAKEFS_CONF_JOB_COMMIT_ID_KEY)
    val apiClient = new ApiClient(conf.get(LAKEFS_CONF_API_URL_KEY),
      conf.get(LAKEFS_CONF_API_ACCESS_KEY_KEY),
      conf.get(LAKEFS_CONF_API_SECRET_KEY_KEY))
    val storageNamespace = apiClient.getRepositoryStorageNamespace(repoName)
    val metaRangeURL = apiClient.getMetaRangeURL(repoName, storageNamespace, commitID)
    val p = new Path(metaRangeURL)
    val fs = p.getFileSystem(job.getConfiguration)
    val localFile = File.createTempFile("lakefs", "metarange")
    fs.copyToLocalFile(p, new Path(localFile.getAbsolutePath))
    val rangesReader = new SSTableReader(localFile.getAbsolutePath, RangeData.newBuilder().build())
    localFile.delete()
    val ranges = read(rangesReader)
    ranges.map(
      r => new GravelerSplit(r.message, new Path(storageNamespace + "/" + apiClient.getRangeURL(repoName, new String(r.id))))
        // Scala / JRE not strong enough to handle List<FileSplit> as List<InputSplit>;
        // explicitly upcast to generate Seq[InputSplit].
        .asInstanceOf[InputSplit]
    )
  }.asJava

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[Array[Byte], WithIdentifier[Catalog.Entry]] = {
    new EntryRecordReader(Catalog.Entry.getDefaultInstance)
  }
}
