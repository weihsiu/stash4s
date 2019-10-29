package stash.kvs

import cats._
import cats.data._
import cats.effect._
import cats.effect.concurrent._
import cats.implicits._
import java.io._
import java.nio.file._
import scala.concurrent.ExecutionContext
import scodec._
import scodec.bits.ByteVector
import stash.kvs.Kvs._
import stash.util._
import monocle.Lens

case class FileKvs[F[_]](
    offsets: Ref[F, Map[ByteVector, Long]],
    output: MVar[F, (Long, OutputStream)],
    inputs: Pool[F, RandomAccessFile]
)

trait HasFileKvs[F[_], A] {
  def fileKvs: Lens[A, FileKvs[F]]
}

object FileKvs {
  def initFileKvs[F[_]: Concurrent: ContextShift](
      file: Path,
      readers: Int
  ): F[FileKvs[F]] = {
    def buildOffsets(
        input: InputStream,
        size: Long,
        offsets: Map[ByteVector, Long],
        offset: Long
    ): F[Map[ByteVector, Long]] =
      for {
        k <- IOs.readLengthBytes(input)
        v <- IOs.readLengthBytes(input)
        offsets2 = if (v.isEmpty) offsets - k else offsets + (k -> (offset + 4 + k.length))
        offset2  = offset + 8 + k.length + v.length
        offsets3 <- if (offset2 >= size) Applicative[F].pure(offsets2)
        else buildOffsets(input, size, offsets2, offset2)
      } yield offsets3
    for {
      size <- Effects.block {
        if (!Files.exists(file)) Files.createFile(file)
        Files.size(file)
      }
      offsets <- Resource
        .make(Effects.block(new BufferedInputStream(new FileInputStream(file.toFile))))(IOs.close(_))
        .use(input => buildOffsets(input, size, Map.empty, 0))
      fileKvs <- (
        Ref.of[F, Map[ByteVector, Long]](offsets),
        MVar.of[F, (Long, OutputStream)](
          (size, new BufferedOutputStream(new FileOutputStream(file.toFile, true)))
        ),
        Pool.of[F, RandomAccessFile](
          List.fill(readers)(new RandomAccessFile(file.toFile, "r"))
        )
      ).mapN(FileKvs[F])
    } yield fileKvs
  }
  def releaseFileKvs[F[_]: ContextShift: Sync](fileKvs: FileKvs[F]): F[Unit] = for {
    (o, os) <- fileKvs.output.take
    _ <- IOs.close(os)
    _ <- fileKvs.output.put((o, os))
    _ <- fileKvs.inputs.release(IOs.close(_))
  } yield ()
  implicit def fileKvs[F[_]: ContextShift: LiftIO: Sync]: Kvs[F, FileKvs[F]] =
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
  implicit def fileKvsOps[F[_]: ContextShift: LiftIO: Sync](
      fileKvs: FileKvs[F]
  ): KvsOps[F, FileKvs[F]] =
    new KvsOps(fileKvs)
}
