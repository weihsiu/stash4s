package stash

import cats._
import cats.data._
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import cats.mtl._
import cats.mtl.implicits._
import stash.kvs._

trait KvsServer[F[_]] {
  def start(interface: String, port: Int): F[Unit]
}

object KvsServer {
  implicit def kvsServer[F[_]: ContextShift: Sync, E](implicit AA: ApplicativeAsk[F, E], HFK: HasFileKvs[F, E]): KvsServer[F] = new KvsServer[F] {
    def start(interface: String, port: Int) = for {
      fileKvs <- AA.ask.map(HFK.fileKvs.get)
      
    } yield ???
  }
}