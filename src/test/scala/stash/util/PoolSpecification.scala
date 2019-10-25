package stash.util

import cats._
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import org.scalacheck._
import scala.concurrent.ExecutionContext.global

object PoolSpecification extends Properties("Pool") {
  import Prop.forAll
  implicit val cs = IO.contextShift(global)
  property("take/put") = forAll(Gen.choose(1, 10)) { n =>
    val test = for {
      pool <- Pool.of[IO, Int](List.range(0, n))
      i <- pool.take
      _ <- pool.put(i)
    } yield true
    test.unsafeRunSync()
  }
}