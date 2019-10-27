package stash.kvs

import cats._
import cats.data._
import cats.effect._
import cats.effect.concurrent._
import cats.implicits._
import java.io._
import scodec._
import scodec.bits.ByteVector
import stash.kvs.Kvs._
import stash.util._

case class FileKvs[F[_]](
    offsets: Ref[F, Map[ByteVector, Long]],
    output: MVar[F, (Long, OutputStream)],
    inputs: Pool[F, RandomAccessFile]
)

object FileKvs {
  implicit def fileKvs[F[_]: ContextShift: FlatMap: LiftIO: Sync]: Kvs[F, FileKvs[F]] =
    new Kvs[F, FileKvs[F]] {
      def insert(x: FileKvs[F], key: ByteVector, value: ByteVector): F[Unit] = {
        assert(value.nonEmpty)
        for {
          (o, os) <- x.output.take
          n       <- IOs.writeLengthBytes(os, key)
          m       <- IOs.writeLengthBytes(os, value)
          _       <- IOs.flush(os)
          _       <- x.offsets.update(_ + (key -> (o + n)))
          _       <- x.output.put((o + n + m, os))
        } yield ()
      }
      def query(x: FileKvs[F], key: ByteVector): F[Option[ByteVector]] =
        (for {
          o <- new OptionT(x.offsets.get.map(_.get(key)))
          bs <- OptionT.liftF(
            Resource
              .make(x.inputs.take)(x.inputs.put)
              .use(
                raf =>
                  for {
                    _  <- IOs.seek(raf, o)
                    bs <- IOs.readLengthBytes(raf)
                  } yield bs
              )
          )
        } yield bs).value
      def remove(x: FileKvs[F], key: ByteVector): F[Unit] =
        for {
          (o, os) <- x.output.take
          n       <- IOs.writeLengthBytes(os, key)
          m       <- IOs.writeLengthBytes(os, ByteVector.empty)
          _       <- IOs.flush(os)
          _       <- x.offsets.update(_ - key)
          _       <- x.output.put((o + n + m, os))
        } yield ()
    }
  implicit def fileKvsOps[F[_]: ContextShift: FlatMap: LiftIO: Sync](
      fileKvs: FileKvs[F]
  ): KvsOps[F, FileKvs[F]] =
    new KvsOps(fileKvs)
}
