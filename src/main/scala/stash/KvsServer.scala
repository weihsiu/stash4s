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
import scodec.codecs._
import scodec.stream._
import stash.Protocols._
import stash.kvs._
import stash.util._
import java.net.InetSocketAddress

trait KvsServer[F[_]] {
  def serve(interface: String, port: Int): F[Unit]
}

object KvsServer {
  def kvsServer[F[_]: Concurrent: ContextShift, G[_]: Monad, E](
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
      socket
        .reads(1024)
        .through(decodeRequest)
        .through(processProtocol(fileKvs))
        .through(encodeResponse)
        .through(socket.writes())
        .onFinalize(socket.endOfOutput)
    private def decodeRequest: Pipe[F, Byte, Request] =
      StreamDecoder.many(requestCodec).toPipeByte
    private def processProtocol(fileKvs: FileKvs[G]): Pipe[F, Request, Response] = _.evalMap {
      case Insert(k, v) => fileKvs.insert(k, v).map[Response](_ => NoneSuccess)
      case Query(k) => fileKvs.query(k).map(_.fold[Response](NoneSuccess)(SomeSuccess(_)))
      case Remove(k) => fileKvs.remove(k).map[Response](_ => NoneSuccess)
    }
    private def encodeResponse: Pipe[F, Response, Byte] =
      StreamEncoder.many(responseCodec).toPipeByte
  }
}
