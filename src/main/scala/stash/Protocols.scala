package stash

import scodec.bits._

object Protocols {
  sealed trait Request
  case class Insert(key: ByteVector, value: ByteVector) extends Request
  case class Query(key: ByteVector) extends Request
  case class Remove(key: ByteVector) extends Request

  sealed trait Response
  case object Success extends Response
  case class SuccessValue(value: ByteVector) extends Response
  case class Failure(error: String) extends Response
}