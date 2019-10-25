package stash.kvs

import cats._
import cats.effect.concurrent.Ref
import scodec.bits.ByteVector

case class FileKvs[F[_]](map: Ref[F, Map[ByteVector, Long]])

object FileKvs {
  implicit def fileKvs[F[_]]: Kvs[F, FileKvs[F]] = new Kvs[F, FileKvs[F]] {
    def insert(x: FileKvs[F], key: ByteVector, value: ByteVector): F[Unit] = ???
    def query(x: FileKvs[F], key: ByteVector): F[Option[ByteVector]] = ???
    def remove(x: FileKvs[F], key: ByteVector): F[Unit] = ???
  }
}
