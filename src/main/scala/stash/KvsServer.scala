package stash

import cats._
import cats.data._
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import cats.mtl._
import cats.mtl.implicits._
import fs2._
import fs2.io.tcp._
import stash.kvs._
import stash.util._
import java.net.InetSocketAddress

trait KvsServer[F[_]] {
  def serve(interface: String, port: Int): F[Unit]
}

object KvsServer {
  implicit def kvsServer[F[_]: Concurrent: ContextShift: Sync, E](
      implicit AA: ApplicativeAsk[F, E],
      HFK: HasFileKvs[F, E]
  ): KvsServer[F] = new KvsServer[F] {
    def serve(interface: String, port: Int) =
      for {
        fileKvs <- AA.ask.map(HFK.fileKvsL.get)
        _ <- (Networks.makeSocketGroup >>= Networks
          .serve(new InetSocketAddress(interface, port), process(fileKvs))).compile.drain
      } yield ()
    def process(fileKvs: FileKvs[F])(socket: Socket[F]): Stream[F, Unit] =
      socket.reads(1024).through(socket.writes()).onFinalize(socket.endOfOutput)
  }
}
