package stash.util

import cats._
import cats.effect._
import cats.effect.concurrent._
import cats.implicits._
import java.io._
import scala.annotation.tailrec
import scodec.codecs.int32
import scodec.bits.ByteVector
import stash.util.Effects._

object IOs {
  trait CanRead[F[_], A] {
    def readByte(x: A): F[Option[Byte]]
    def readBytes(x: A, n: Int): F[ByteVector]
  }
  implicit def inputStreamCanRead[F[_]: ContextShift: Sync]: CanRead[F, InputStream] =
    new CanRead[F, InputStream] {
      def readByte(x: InputStream) = block {
        val b = x.read()
        if (b == -1) None else Some(b.toByte)
      }
      def readBytes(x: InputStream, n: Int) = block {
        @tailrec def loop(bs: Array[Byte], o: Int, n: Int): Unit = {
          val l = x.read(bs, o, n)
          if (l > 0 && l < n) loop(bs, o + l, n - l)
        }
        val bs = Array.ofDim[Byte](n)
        loop(bs, 0, n)
        ByteVector(bs)
      }
    }
  implicit def randomAccessFileCanRead[F[_]: ContextShift: Sync]: CanRead[F, RandomAccessFile] =
    new CanRead[F, RandomAccessFile] {
      def readByte(x: RandomAccessFile) = block {
        val b = x.read()
        if (b == -1) None else Some(b.toByte)
      }
      def readBytes(x: RandomAccessFile, n: Int) = block {
        @tailrec def loop(bs: Array[Byte], o: Int, n: Int): Unit = {
          val l = x.read(bs, o, n)
          if (l > 0 && l < n) loop(bs, o + l, n - l)
        }
        val bs = Array.ofDim[Byte](n)
        loop(bs, 0, n)
        ByteVector(bs)
      }
    }
  trait CanWrite[F[_], A] {
    def writeBytes(x: A, bs: ByteVector): F[Unit]
    def flush(x: A): F[Unit]
  }
  implicit def outputStreamCanWrite[F[_]: ContextShift: Sync]: CanWrite[F, OutputStream] =
    new CanWrite[F, OutputStream] {
      def writeBytes(x: OutputStream, bs: ByteVector) = block(x.write(bs.toArray))
      def flush(x: OutputStream)                      = block(x.flush())
    }
  implicit def randomAccessFileCanWrite[F[_]: ContextShift: Sync]: CanWrite[F, RandomAccessFile] =
    new CanWrite[F, RandomAccessFile] {
      def writeBytes(x: RandomAccessFile, bs: ByteVector) =
        block(x.write(bs.toArray))
      def flush(x: RandomAccessFile) = Applicative[F].pure(())
    }
  trait CanSeek[F[_], A] {
    def seek(x: A, to: Long): F[Unit]
  }
  implicit def randomAccessFileCanSeek[F[_]: ContextShift: Sync]: CanSeek[F, RandomAccessFile] =
    new CanSeek[F, RandomAccessFile] {
      def seek(x: RandomAccessFile, to: Long) = block(x.seek(to))
    }
  def readLengthBytes[F[_]: ContextShift: FlatMap: LiftIO: Sync, A](
      x: A
  )(implicit CR: CanRead[F, A]): F[ByteVector] =
    for {
      n <- CR
        .readBytes(x, 4)
        .map(bs => int32.decodeValue(bs.toBitVector).require)
      bs <- CR.readBytes(x, n)
    } yield bs
  def writeLengthBytes[F[_]: ContextShift: FlatMap: Sync, A](
      x: A,
      bs: ByteVector
  )(implicit CW: CanWrite[F, A]): F[Int] =
    for {
      _ <- block(
        CW.writeBytes(x, int32.encode(bs.length.toInt).require.toByteVector)
      )
      _ <- block(CW.writeBytes(x, bs))
    } yield 4 + bs.length.toInt
  def flush[F[_]: ContextShift: Sync, A](x: A)(implicit CW: CanWrite[F, A]): F[Unit] = CW.flush(x)
  def seek[F[_]: ContextShift: Sync, A](x: A, to: Long)(implicit CS: CanSeek[F, A]): F[Unit] =
    CS.seek(x, to)
}
