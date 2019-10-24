package stash.kvs

import cats._
import cats.effect._
import cats.effect.concurrent._
import cats.effect.implicits._
import cats.implicits._
import org.scalacheck._
import scodec.bits.ByteVector
import stash.Arbitraries._
import stash.kvs.Kvs
import stash.kvs.MapKvs

object MapKvsSpecification extends Properties("MapKvs") {
  import Prop.forAll
  implicit val F = Sync[IO]
  property("insert/query/remove") = forAll { (k: ByteVector, v: ByteVector) =>
    val test = for {
      kvs <- Ref.of(Map.empty[ByteVector, ByteVector])
      mapKvs = MapKvs(kvs)
      _ <- mapKvs.insert(k, v)
      r <- mapKvs.query(k)
    } yield r == v
    test.unsafeRunSync()
  }
}