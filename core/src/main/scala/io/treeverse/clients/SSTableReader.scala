package io.treeverse.clients

import com.google.protobuf.{CodedInputStream, Message}
import org.rocksdb.{SstFileReader, _}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion}

import java.io.{ByteArrayInputStream, Closeable, DataInputStream}
import scala.collection.JavaConverters._

class Item[T](val key: Array[Byte], val id: Array[Byte], val message: T)

private object local {
  def readNBytes(s: DataInputStream, n: Int): Array[Byte] = {
    val ret = new Array[Byte](n)
    s.readFully(ret)
    ret
  }
}

class SSTableIterator[T <: GeneratedMessage](val it: SstFileReaderIterator, messagePrototype: GeneratedMessageCompanion[T]) extends Iterator[Item[T]] with Closeable {
  // TODO(ariels): explicitly make it closeable, and figure out how to close it when used by
  //     Spark.
  override def hasNext: Boolean = it.isValid

  override def close = it.close

  override def next(): Item[T] = {
    val bais = new ByteArrayInputStream(it.value)
    val key = it.key()
    val dis = new DataInputStream(bais)
    val identityLength = VarInt.readSignedVarLong(dis)
    val id = local.readNBytes(dis, identityLength.toInt)
    val dataLength = VarInt.readSignedVarLong(dis)
    val data = local.readNBytes(dis, dataLength.toInt)
    // TODO(ariels): Error if dis is not exactly at end?  (But then cannot add more fields in
    //     future, sigh...)

    val dataStream = CodedInputStream.newInstance(data)
    val message = messagePrototype.parseFrom(dataStream)

    // TODO (johnnyaug) validate item is of the expected type - metarange/range
    dataStream.checkLastTagWas(0)
    it.next()

    new Item(key, id, message)
  }
}

object SSTableReader {
  RocksDB.loadLibrary()
}

class SSTableReader[T <: GeneratedMessage](sstableFile: String, messagePrototype: GeneratedMessageCompanion[T]) extends Closeable {
  private val options = new Options
  private val reader = new SstFileReader(options)
  private val readOptions = new ReadOptions
  reader.open(sstableFile)

  def close() = {
    reader.close
    options.close
    readOptions.close
  }

  def getMetadata(): Map[String, String] = reader.getTableProperties.getUserCollectedProperties.asScala.toMap

  def newIterator(): SSTableIterator[T] = {
    val it = reader.newIterator(readOptions)
    it.seekToFirst()
    new SSTableIterator(it, messagePrototype)
  }
}
