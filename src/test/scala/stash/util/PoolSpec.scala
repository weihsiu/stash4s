package stash.util

import cats._
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import org.scalacheck._
import org.scalatest._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.global

class PoolSpec extends FlatSpec with Matchers {
  "a Pool of Ints" should "take and put correctly" in {
    implicit val cs = IO.contextShift(global)
    implicit val t = IO.timer(global)
    def sleepFor(pool: Pool[IO, Int]): IO[Int] = for {
      n <- pool.take
      _ <- IO.sleep(n.seconds)
      _ <- pool.put(n)
    } yield n
    val test = for {
      pool <- Pool.of[IO, Int](List(1, 2, 3))
      rs <- (1 to 10).toList.map(_ => sleepFor(pool)).parSequence
    } yield rs
    assert(test.unsafeRunSync().groupBy(identity).view.mapValues(_.length).toMap == Map((1, 5), (2, 3), (3, 2)))
  }
}