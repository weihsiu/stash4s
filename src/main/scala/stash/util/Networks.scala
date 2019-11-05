package stash.util

import cats.effect._
import fs2._
import fs2.io.tcp._
import java.net.InetSocketAddress

object Networks {
  def makeSocketGroup[F[_]: ContextShift: Sync]: Stream[F, SocketGroup] =
    Stream.resource(Blocker[F].flatMap(SocketGroup[F](_)))
  def serve[F[_]: Concurrent: ContextShift](
      address: InetSocketAddress,
      process: Socket[F] => Stream[F, Unit]
  ): SocketGroup => Stream[F, Unit] =
    _.server(address).flatMap(Stream.resource(_).map(process)).parJoinUnbounded
  def connect[F[_]: Concurrent: ContextShift](
      address: InetSocketAddress,
      process: Socket[F] => Stream[F, Unit]
  ): SocketGroup => Stream[F, Unit] =
    sg => Stream.resource(sg.client(address)).flatMap(process)
}
