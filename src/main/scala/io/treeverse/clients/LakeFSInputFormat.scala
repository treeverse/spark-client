package io.treeverse.clients

import io.treeverse.committed.Committed.RangeData
import io.treeverse.catalog.Catalog

import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.{InputSplit, RecordReader, TaskAttemptContext}
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat
import org.apache.hadoop.mapreduce.InputFormat
import org.apache.hadoop.mapred.SplitLocationInfo
import org.apache.hadoop.mapreduce.JobContext
import org.apache.hadoop.mapreduce.lib.input.FileSplit

import java.io.File

import scala.collection.JavaConverters._
import com.google.protobuf.Message

class GravelerSplit(val range: RangeData) extends InputSplit {
  override def getLength: Long = range.getEstimatedSize

  override def getLocations: Array[String] = null

  override def getLocationInfo(): Array[SplitLocationInfo] = null
}

class WithIdentifier[Proto <: Message](val id: Array[Byte], val message: Proto) {}

class EntryRecordReader[Proto <: Message](ssTableReader: SSTableReader[Proto]) extends RecordReader[Array[Byte], WithIdentifier[Proto]] {
  var it: SSTableIterator[Proto] = null
  var item: Item[Proto] = null

  override def initialize(split: InputSplit, context: TaskAttemptContext) = {
    val range = split.asInstanceOf[GravelerSplit]
    it = ssTableReader.newIterator()
  }

  override def nextKeyValue: Boolean = {
    if (!it.hasNext) {
      return false
    }
    item = it.next
    return true
  }

  override def getCurrentKey = item.key

  override def getCurrentValue() = new WithIdentifier(item.id, item.message)

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
    val metaRangePath = job.getConfiguration.get(FileInputFormat.INPUT_DIR)
    val p = new Path(metaRangePath)
    val fs = p.getFileSystem(job.getConfiguration)
    val localFile = File.createTempFile("lakefs", "metarange")
    fs.copyToLocalFile(p, new Path(localFile.getAbsolutePath))
    // TODO(johnnyaug) ask lakeFS metadata server for the sstable url
    val rangesReader = new SSTableReader(localFile.getAbsolutePath, RangeData.newBuilder().build())
    localFile.delete()

    val ranges = read(rangesReader)

    ranges.map(
      r => new FileSplit(new Path(p.getParent + "/" + new String(r.id)), 0, 0, null.asInstanceOf[Array[String]])
        // Scala / JRE not strong enough to handle List<FileSplit> as List<InputSplit>;
        // explicitly upcast to generate Seq[InputSplit].
        .asInstanceOf[InputSplit]
    ).asJava
  }

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[Array[Byte], WithIdentifier[Catalog.Entry]] = {
    val localFile = File.createTempFile("lakefs", "range")
    localFile.deleteOnExit()
    val fileSplit = split.asInstanceOf[FileSplit]
    val fs = fileSplit.getPath.getFileSystem(context.getConfiguration)
    fs.copyToLocalFile(fileSplit.getPath, new Path(localFile.getAbsolutePath))
    // TODO(johnnyaug) should we cache this?
    new EntryRecordReader(new SSTableReader(localFile.getAbsolutePath, Catalog.Entry.getDefaultInstance))
  }
}
