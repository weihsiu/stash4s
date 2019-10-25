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
  property("insert/query/remove") = forAll { (k: ByteVector, v: ByteVector) =>
    val test = for {
      mapKvs <- Ref.of[IO, Map[ByteVector, ByteVector]](Map.empty).map(MapKvs(_))
      _ <- mapKvs.insert(k, v)
      r1 <- mapKvs.query(k)
      _ <- mapKvs.remove(k)
      r2 <- mapKvs.query(k)
    } yield r1 == Some(v) && r2 == None
    test.unsafeRunSync()
  }
}