package stash.kvs

import scodec.bits.ByteVector

trait Kvs[F[_], A] {
  def insert(x: A, key: ByteVector, value: ByteVector): F[Unit]
  def query(x: A, key: ByteVector): F[Option[ByteVector]]
  def remove(x: A, key: ByteVector): F[Unit]
}
