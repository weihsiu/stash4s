package stash.util

import cats._
import cats.effect._
import cats.effect.concurrent._
import cats.implicits._
import cats.mtl._
import cats.mtl.implicits._
import java.io._
import monocle.Lens
import scala.annotation.tailrec
import scodec.codecs.int32
import scodec.bits.ByteVector
import stash.util.Effects._

object IOs {
  trait CanRead[F[_], -A] {
    def readByte(x: A, b: Blocker): F[Option[Byte]]
    def readBytes(x: A, b: Blocker, n: Int): F[ByteVector]
  }
  implicit def inputStreamCanRead[F[_]: ContextShift: Sync]: CanRead[F, InputStream] =
    new CanRead[F, InputStream] {
      def readByte(x: InputStream, b: Blocker) = b.delay {
        val b = x.read()
        if (b == -1) None else Some(b.toByte)
      }
      def readBytes(x: InputStream, b: Blocker, n: Int) = b.delay {
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
      def readByte(x: RandomAccessFile, b: Blocker) = b.delay {
        val b = x.read()
        if (b == -1) None else Some(b.toByte)
      }
      def readBytes(x: RandomAccessFile, b: Blocker, n: Int) = b.delay {
        @tailrec def loop(bs: Array[Byte], o: Int, n: Int): Unit = {
          val l = x.read(bs, o, n)
          if (l > 0 && l < n) loop(bs, o + l, n - l)
        }
        val bs = Array.ofDim[Byte](n)
        loop(bs, 0, n)
        ByteVector(bs)
      }
    }

  trait CanWrite[F[_], -A] {
    def writeBytes(x: A, b: Blocker, bs: ByteVector): F[Unit]
    def flush(x: A, b: Blocker): F[Unit]
  }
  implicit def outputStreamCanWrite[F[_]: ContextShift: Sync]: CanWrite[F, OutputStream] =
    new CanWrite[F, OutputStream] {
      def writeBytes(x: OutputStream, b: Blocker, bs: ByteVector) = b.delay(x.write(bs.toArray))
      def flush(x: OutputStream, b: Blocker)                      = b.delay(x.flush())
    }
  implicit def randomAccessFileCanWrite[F[_]: ContextShift: Sync]: CanWrite[F, RandomAccessFile] =
    new CanWrite[F, RandomAccessFile] {
      def writeBytes(x: RandomAccessFile, b: Blocker, bs: ByteVector) =
        b.delay(x.write(bs.toArray))
      def flush(x: RandomAccessFile, b: Blocker) = Applicative[F].unit
    }

  trait CanSeek[F[_], -A] {
    def seek(x: A, b: Blocker, to: Long): F[Unit]
  }
  implicit def randomAccessFileCanSeek[F[_]: ContextShift: Sync]: CanSeek[F, RandomAccessFile] =
    new CanSeek[F, RandomAccessFile] {
      def seek(x: RandomAccessFile, b: Blocker, to: Long) = b.delay(x.seek(to))
    }

  trait CanClose[F[_], -A] {
    def close(x: A, b: Blocker): F[Unit]
  }
  implicit def closeableCanClose[F[_]: ContextShift: Sync]: CanClose[F, Closeable] =
    new CanClose[F, Closeable] {
      def close(x: Closeable, b: Blocker) = b.delay(x.close())
    }

  def readLengthBytes[F[_]: ContextShift: FlatMap: LiftIO: Sync, A](
      x: A, b: Blocker
  )(implicit CR: CanRead[F, A]): F[ByteVector] =
    for {
      n <- CR
        .readBytes(x, b, 4)
        .map(bs => int32.decodeValue(bs.toBitVector).require)
      bs <- CR.readBytes(x, b, n)
    } yield bs
  def writeLengthBytes[F[_]: ContextShift: FlatMap: Sync, A](
      x: A,
      b: Blocker,
      bs: ByteVector
  )(implicit CW: CanWrite[F, A]): F[Int] =
    for {
      _ <- CW.writeBytes(x, b, int32.encode(bs.length.toInt).require.toByteVector)
      _ <- CW.writeBytes(x, b, bs)
    } yield 4 + bs.length.toInt
  def flush[F[_]: ContextShift: Sync, A](x: A, b: Blocker)(implicit CW: CanWrite[F, A]): F[Unit] = CW.flush(x, b)
  def seek[F[_]: ContextShift: Sync, A](x: A, b: Blocker, to: Long)(implicit CS: CanSeek[F, A]): F[Unit] =
    CS.seek(x, b, to)
  def close[F[_]: ContextShift: Sync, A](x: A, b: Blocker)(implicit CC: CanClose[F, A]): F[Unit] = CC.close(x, b)
}
