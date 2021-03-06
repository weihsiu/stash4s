package stash

import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import cats.mtl._
import cats.mtl.implicits._

object RIOs {
  type RIO[E, A] = Kleisli[IO, E, A]
  def liftRIO[F[_]: ApplicativeAsk[*[_], E]: FlatMap: LiftIO, E, A](rio: RIO[E, A]): F[A] =
    ApplicativeAsk[F, E].ask >>= (env => LiftIO[F].liftIO(rio.run(env)))
  implicit def io2RIO[E]: IO ~> RIO[E, *] = Lambda[IO ~> RIO[E, *]](Kleisli.liftF(_))
}
