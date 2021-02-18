  package io.treeverse.clients

import io.treeverse.committed.Committed.RangeData
import org.apache.hadoop.fs.Path
import org.apache.hadoop.io.Text
import org.apache.hadoop.mapred._ // TODO (johnnyaug)  use only "madereduce", not mapred
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat

import java.io.File

class EntryRecordReader(ssTableReader: SSTableReader, localFile: String) extends RecordReader[Text, Text] {
  private val it = ssTableReader.getData(localFile)

  override def next(key: Text, value: Text): Boolean = {
    if (!it.hasNext) {
      return false
    }
    key.set(it.next().key)
    true
  }

  override def createKey(): Text = {
    new Text()
  }

  override def createValue(): Text = {
    new Text()
  }

  override def getPos: Long = {
    0 // TODO(johnnyaug) complete
  }

  override def close(): Unit = {
    new File(localFile).delete
  }

  override def getProgress: Float = {
    0 // TODO(johnnyaug) complete
  }
}

object LakeFSInputFormat {}

class LakeFSInputFormat extends InputFormat[Text, Text] {
  override def getSplits(job: JobConf, numSplits: Int): Array[InputSplit] = {
    val metaRangePath = job.get(FileInputFormat.INPUT_DIR)
    val p = new Path(metaRangePath)
    val fs = p.getFileSystem(job)
    val localFile = File.createTempFile("lakefs", "metarange")
    fs.copyToLocalFile(p, new Path(localFile.getAbsolutePath))
    val ranges = new SSTableReader().get(localFile.getAbsolutePath, RangeData.newBuilder().build())
    localFile.delete()
    // TODO(johnnyaug) ask lakeFS metadata server for the sstable url
    ranges.map(r => new FileSplit(new Path(p.getParent + "/" + new String(r.identity)), 0, 0, null.asInstanceOf[Array[String]])).toArray
  }

  override def getRecordReader(split: InputSplit, job: JobConf, reporter: Reporter): RecordReader[Text, Text] = {
    val localFile = File.createTempFile("lakefs", "range")
    localFile.deleteOnExit()
    val fileSplit = split.asInstanceOf[FileSplit]
    val fs = fileSplit.getPath.getFileSystem(job)
    fs.copyToLocalFile(fileSplit.getPath, new Path(localFile.getAbsolutePath))
    // TODO(johnnyaug) should we cache this?
    new EntryRecordReader(new SSTableReader(), localFile.getAbsolutePath)
  }

}
