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
import scodec.stream._
import stash.kvs._
import stash.util._
import java.net.InetSocketAddress

trait KvsServer[F[_]] {
  def serve(interface: String, port: Int): F[Unit]
}

object KvsServer {
  def kvsServer[F[_]: Concurrent: ContextShift: Sync, G[_]: Monad, E](
      implicit AA: ApplicativeAsk[G, E],
      G2F: G ~> F, 
      HFK: HasFileKvs[G, E]
  ): KvsServer[F] = new KvsServer[F] {
    def serve(interface: String, port: Int) =
      for {
        fileKvs <- G2F(AA.ask.map(HFK.fileKvsL.get))
        _ <- (Networks.makeSocketGroup >>= Networks
          .serve(new InetSocketAddress(interface, port), process(fileKvs))).compile.drain
      } yield ()
    private def process(fileKvs: FileKvs[G])(socket: Socket[F]): Stream[F, Unit] =
      socket.reads(1024).through
      // socket.reads(1024).through(socket.writes()).onFinalize(socket.endOfOutput)
    private def decodeRequests(fileKvs: FileKvs[G]): Pipe[F, Byte, Byte] =
      StreamDecoder.many()
  }
}
