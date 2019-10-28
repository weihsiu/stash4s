package stash.util

import cats._
import cats.effect.Concurrent
import cats.effect.concurrent.{MVar, Semaphore}
import cats.implicits._

trait Pool[F[_], A] {
  def take: F[A]
  def put(x: A): F[Unit]
  def release(f: A => F[Unit]): F[Unit]
}

object Pool {
  def of[F[_] : Concurrent, A](xs: List[A]): F[Pool[F, A]] = for {
    s <- Semaphore(1)
    m <- MVar.of(xs)
    r <- MVar.empty[F, A => F[Unit]]
  } yield new Pool[F, A] {
    def take: F[A] = for {
      x :: xs <- m.take
      _ <- Applicative[F].whenA(xs.nonEmpty)(m.put(xs))
    } yield x
    def put(x: A): F[Unit] =
      s.withPermit(for {
        _ <- m.tryTake >>= (_.fold(m.put(List(x)))(xs => m.put(x :: xs)))
        _ <- r.tryTake >>= (_.fold(Applicative[F].unit)(f => r.put(f) *> f(x)))
      } yield ())
    def release(f: A => F[Unit]): F[Unit] =
      s.withPermit(for {
        _ <- m.tryTake >>= (_.fold(Applicative[F].unit)(xs => m.put(xs) *> xs.map(f).sequence_))
        _ <- r.put(f)
      } yield ())
  }
}