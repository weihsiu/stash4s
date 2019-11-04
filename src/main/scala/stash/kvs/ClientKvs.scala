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
import stash.Protocols._
import stash.util.Networks

case class ClientKvs[F[_]](channels: ClientKvs.Channels[F])

trait HasClientKvs[F[_], A] {
  def clientKvsL: Lens[A, ClientKvs[F]]
}

object ClientKvs {
  type Channels[F[_]] = MVar[F, (MVar[F, Request], MVar[F, Response])]
  def initClientKvs[F[_]: Concurrent: ContextShift](host: String, port: Int): F[ClientKvs[F]] = {
    def decodeResponse: Pipe[F, Byte, Response] = StreamDecoder.many(responseCodec).toPipeByte
    def encodeRequest: Pipe[F, Request, Byte]   = StreamEncoder.many(requestCodec).toPipeByte
    def process(cs: Channels[F])(s: Socket[F]): Stream[F, Unit] = {
      (Stream.emit(Primer) ++ s.reads(1024).through(decodeResponse))
        .evalMap(
          rp =>
            Resource.make(cs.take)(cs.put).use {
              case (rqs, rps) =>
                for {
                  _  <- if (rp == Primer) Applicative[F].pure(()) else rps.put(rp)
                  rq <- rqs.take
                } yield rq
            }
        )
        .through(encodeRequest)
        .through(s.writes())
        .onFinalize(s.endOfOutput)
    }
    for {
      rqs <- MVar.empty[F, Request]
      rps <- MVar.empty[F, Response]
      cs  <- MVar.of((rqs, rps))
      _ <- (Networks.makeSocketGroup >>= Networks
        .connect(new InetSocketAddress(host, port), process(cs))).compile.drain
    } yield ClientKvs(cs)
  }
  implicit def clientKvs[F[_]: Bracket[*[_], Throwable]: FlatMap]: Kvs[F, ClientKvs[F]] =
    new Kvs[F, ClientKvs[F]] {
      def insert(x: ClientKvs[F], key: ByteVector, value: ByteVector): F[Unit] =
        Resource.make(x.channels.take)(x.channels.put).use {
          case (rqs, rps) =>
            for {
              _  <- rqs.put(Insert(key, value))
              rp <- rps.take
              _ = assert(rp == NoneSuccess)
            } yield ()
        }
      def query(x: ClientKvs[F], key: ByteVector): F[Option[ByteVector]] =
        Resource.make(x.channels.take)(x.channels.put).use {
          case (rqs, rps) =>
            for {
              _  <- rqs.put(Query(key))
              rp <- rps.take
            } yield rp match {
              case NoneSuccess    => None
              case SomeSuccess(v) => Some(v)
            }
        }
      def remove(x: ClientKvs[F], key: ByteVector): F[Unit] =
        Resource.make(x.channels.take)(x.channels.put).use {
          case (rqs, rps) =>
            for {
              _  <- rqs.put(Remove(key))
              rp <- rps.take
              _ = assert(rp == NoneSuccess)
            } yield ()
        }
    }
}
