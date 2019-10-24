package stash.kvs

import cats._
import cats.implicits._
import cats.effect._
import cats.effect.concurrent.Ref
import scodec.bits.ByteVector

case class MapKvs[F[_]](kvs: Ref[F, Map[ByteVector, ByteVector]])

object MapKvs {
  implicit def mapKvs[F[_] : Functor : Sync]: Kvs[F, MapKvs[F]] = new Kvs[F, MapKvs[F]] {
    def insert(x: MapKvs[F], key: ByteVector, value: ByteVector): F[Unit] =
      x.kvs.update(_ + (key -> value))
    def query(x: MapKvs[F], key: ByteVector): F[Option[ByteVector]] =
      x.kvs.get.map(_.get(key))
    def remove(x: MapKvs[F], key: ByteVector): F[Unit] =
      x.kvs.update(_ - key)
  }
}