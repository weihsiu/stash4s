package stash.kvs

import cats._
import cats.effect._
import cats.effect.concurrent._
import cats.implicits._
import java.io._
import scodec._
import scodec.bits.ByteVector
import stash.util._

case class FileKvs[F[_]](
    offsets: Ref[F, Map[ByteVector, Long]],
    output: MVar[F, (Long, OutputStream)],
    inputs: Pool[F, InputStream]
)

object FileKvs {
  implicit def fileKvs[F[_] : ContextShift : FlatMap : LiftIO : Sync]: Kvs[F, FileKvs[F]] = new Kvs[F, FileKvs[F]] {
    def insert(x: FileKvs[F], key: ByteVector, value: ByteVector): F[Unit] = for {
      (o, os) <- x.output.take
      keyBytes = Codecs.nBytes.encode(key).require.toByteArray
      _ <- Effects.block(os.write(keyBytes))
      valueBytes = Codecs.nBytes.encode(value).require.toByteArray
      _ <- Effects.block(os.write(valueBytes))
      _ <- x.offsets.update(_ + (key -> (o + keyBytes.length)))
      _ <- x.output.put((o + keyBytes.length + valueBytes.length, os))
    } yield ()
    def query(x: FileKvs[F], key: ByteVector): F[Option[ByteVector]] = ???
    def remove(x: FileKvs[F], key: ByteVector): F[Unit] = ???
  }
}
