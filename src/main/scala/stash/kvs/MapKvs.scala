package stash.kvs

import cats._
import cats.implicits._
import cats.effect._
import cats.effect.concurrent.Ref
import scodec.bits.ByteVector
import stash.kvs.Kvs._

case class MapKvs[F[_]](map: Ref[F, Map[ByteVector, ByteVector]])

object MapKvs {
  implicit def mapKvs[F[_]: Sync]: Kvs[F, MapKvs[F]] = new Kvs[F, MapKvs[F]] {
    def insert(x: MapKvs[F], key: ByteVector, value: ByteVector): F[Unit] =
      x.map.update(_ + (key -> value))
    def query(x: MapKvs[F], key: ByteVector): F[Option[ByteVector]] =
      x.map.get.map(_.get(key))
    def remove(x: MapKvs[F], key: ByteVector): F[Unit] =
      x.map.update(_ - key)
  }
  implicit def mapKvsOps[F[_]: Sync](mapKvs: MapKvs[F]): KvsOps[F, MapKvs[F]] = new KvsOps(mapKvs)
}
