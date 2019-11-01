package stash

import scodec._
import scodec.codecs._
import scodec.bits._

object Protocols {
  val nBytesCodec = variableSizeBytes(int32, bytes)

  sealed trait Request
  case class Insert(key: ByteVector, value: ByteVector) extends Request
  case class Query(key: ByteVector) extends Request
  case class Remove(key: ByteVector) extends Request

  val requestCodec: Codec[Request] = discriminated[Request].by(uint8)
    .| (0) { case Insert(k, v) => (k, v) } (Insert.tupled) (nBytesCodec ~ nBytesCodec)
    .| (1) { case Query(k) => k } (Query) (nBytesCodec)
    .| (2) { case Remove(k) => k } (Remove) (nBytesCodec)

  sealed trait Response
  case object NoneSuccess extends Response
  case class SomeSuccess(value: ByteVector) extends Response
  case class Failure(error: String) extends Response

  val responseCodec: Codec[Response] = discriminated[Response].by(uint8)
    .| (0) { case NoneSuccess => NoneSuccess} (identity) (ignore(0).exmap(_ => Attempt.successful(NoneSuccess), _ => Attempt.successful(())))
    .| (1) { case SomeSuccess(v) => v } (SomeSuccess) (nBytesCodec)
    .| (2) { case Failure(e) => e } (Failure) (utf8_32)
}