package stash.kvs

import scodec.bits.ByteVector

trait Kvs[F[_], A] {
  def insert(x: A, key: ByteVector, value: ByteVector): F[Unit]
  def query(x: A, key: ByteVector): F[Option[ByteVector]]
  def remove(x: A, key: ByteVector): F[Unit]
}

object Kvs {
  implicit class ImplicitKvs[F[_], A](x: A)(implicit K: Kvs[F, A]) {
    def insert(key: ByteVector, value: ByteVector): F[Unit] = K.insert(x, key, value)
    def query(key: ByteVector): F[Option[ByteVector]] = K.query(x, key)
    def remove(key: ByteVector): F[Unit] = K.remove(x, key)
  }
}