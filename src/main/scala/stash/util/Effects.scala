package stash.util

import cats.effect._
import cats.effect.concurrent._

object Effects {
  def block[F[_]: ContextShift: LiftIO: Sync, A](thunk: => A): F[A] =
    Blocker[F].use(_.delay(thunk))
}
