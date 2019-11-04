package stash.kvs

import cats._
import cats.effect._
import cats.effect.concurrent._
import cats.implicits._
import fs2._
import fs2.io.tcp._
import java.net.InetSocketAddress
import monocle._
import scodec.bits.ByteVector
import scodec.codecs._
import scodec.stream._
import stash.kvs.Kvs._
import stash.Protocols._
import stash.util.Networks

case class ClientKvs[F[_]](request: MVar[F, Request], response: MVar[F, Response])

trait HasClientKvs[F[_], A] {
  def clientKvsL: Lens[A, ClientKvs[F]]
}

object ClientKvs {
  def initClientKvs[F[_]: Concurrent: ContextShift](host: String, port: Int): F[ClientKvs[F]] = {
    def decodeResponse: Pipe[F, Byte, Response] = StreamDecoder.many(responseCodec).toPipeByte
    def encodeRequest: Pipe[F, Request, Byte]   = StreamEncoder.many(requestCodec).toPipeByte
    def process(request: MVar[F, Request], response: MVar[F, Response])(s: Socket[F]): Stream[F, Unit] =
      (Stream.emit(Primer) ++ s.reads(1024).through(decodeResponse))
        .evalMap(
          rp =>
            for {
              _  <- if (rp == Primer) Applicative[F].pure(()) else response.put(rp)
              rq <- request.take
            } yield rq
        )
        .through(encodeRequest)
        .through(s.writes())
        .onFinalize(s.endOfOutput)
    for {
      request <- MVar.empty[F, Request]
      response <- MVar.empty[F, Response]
      _ <- Concurrent[F].start(
        (Networks.makeSocketGroup >>= Networks
          .connect(new InetSocketAddress(host, port), process(request, response))).compile.drain
      )
    } yield ClientKvs(request, response)
  }
  implicit def clientKvs[F[_]: Bracket[*[_], Throwable]: FlatMap](
      implicit ME: MonadError[F, Throwable]
  ): Kvs[F, ClientKvs[F]] =
    new Kvs[F, ClientKvs[F]] {
      def insert(x: ClientKvs[F], key: ByteVector, value: ByteVector): F[Unit] =
        for {
          _  <- x.request.put(Insert(key, value))
          rp <- x.response.take
          _ <- if (rp == NoneSuccess) ().pure[F]
          else ME.raiseError(new RuntimeException(s"unexpected response: $rp"))
        } yield ()
      def query(x: ClientKvs[F], key: ByteVector): F[Option[ByteVector]] =
        for {
          _  <- x.request.put(Query(key))
          rp <- x.response.take
          r <- rp match {
            case Primer         => ME.raiseError(new RuntimeException("unexpected response: Primer"))
            case NoneSuccess    => none.pure[F]
            case SomeSuccess(v) => v.some.pure[F]
            case Failure(e)     => ME.raiseError(new RuntimeException(e))
          }
        } yield r
      def remove(x: ClientKvs[F], key: ByteVector): F[Unit] =
        for {
          _  <- x.request.put(Remove(key))
          rp <- x.response.take
          _ <- if (rp == NoneSuccess) ().pure[F]
          else ME.raiseError(new RuntimeException(s"unexpected response: $rp"))
        } yield ()
    }
  implicit def clientKvsOps[F[_]: Bracket[*[_], Throwable]: FlatMap](
      clientKvs: ClientKvs[F]
  ): KvsOps[F, ClientKvs[F]] =
    new KvsOps(clientKvs)
}
