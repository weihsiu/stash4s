package stash.util

import scodec.bits._
import scodec._
import scodec.codecs._

object Codecs {
  val nBytes: Codec[ByteVector] = variableSizeBytes(int32, bytes)
}