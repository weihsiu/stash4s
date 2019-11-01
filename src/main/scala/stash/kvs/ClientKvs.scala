package stash.kvs

import cats.effect.concurrent._
import fs2._
import fs2.io.tcp._
import monocle._
import scodec.bits.ByteVector

case class ClientKvs[F[_]](socket: Deferred[F, Socket[F]])

trait HasClientKvs[F[_], A] {
  def clientKvsL: Lens[A, ClientKvs[F]]
}

object ClientKvs {
  def initClientKvs[F[_]](host: String, port: Int): F[ClientKvs[F]] = ???
  implicit def clientKvs[F[_], G[_]]: Kvs[F, ClientKvs[G]] = new Kvs[F, ClientKvs[G]] {

    def insert(x: ClientKvs[G], key: ByteVector, value: ByteVector): F[Unit] = ???
    def query(x: ClientKvs[G], key: ByteVector): F[Option[ByteVector]] = ???
    def remove(x: ClientKvs[G], key: ByteVector): F[Unit] = ???
  }
}