package stash.util

import cats._
import cats.effect.Concurrent
import cats.effect.concurrent.{MVar, Semaphore}
import cats.implicits._

trait Pool[F[_], A] {
  def take: F[A]
  def put(item: A): F[Unit]
}

object Pool {
  def of[F[_] : Concurrent, A](items: List[A]): F[Pool[F, A]] = for {
    s <- Semaphore(1)
    m <- MVar.of(items)
  } yield new Pool[F, A] {
    def take: F[A] = for {
      x :: xs <- m.take
      _ <- Applicative[F].whenA(xs.nonEmpty)(m.put(xs))
    } yield x
    def put(item: A): F[Unit] =
      s.withPermit(m.tryTake >>= (_.fold(m.put(List(item)))(xs => m.put(item :: xs))))
  }
}