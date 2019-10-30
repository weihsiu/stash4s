package stash.util

import cats.effect._
import fs2._
import fs2.io.tcp._

object Networks {
  def mkSocketGroup[F[_]: ContextShift: Sync]: Stream[F, SocketGroup] =
    Stream.resource(Blocker[F].flatMap(SocketGroup[F](_)))
}