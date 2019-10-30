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
  trait CanSyncRead[F[_], -A] {
    def syncReadByte(x: A, b: Blocker): F[Option[Byte]]
    def syncReadBytes(x: A, b: Blocker, n: Int): F[ByteVector]
  }
  implicit def inputStreamCanSyncRead[F[_]: ContextShift: Sync]: CanSyncRead[F, InputStream] =
    new CanSyncRead[F, InputStream] {
      def syncReadByte(x: InputStream, b: Blocker) = b.delay {
        val b = x.read()
        if (b == -1) None else Some(b.toByte)
      }
      def syncReadBytes(x: InputStream, b: Blocker, n: Int) = b.delay {
        @tailrec def loop(bs: Array[Byte], o: Int, n: Int): Unit = {
          val l = x.read(bs, o, n)
          if (l > 0 && l < n) loop(bs, o + l, n - l)
        }
        val bs = Array.ofDim[Byte](n)
        loop(bs, 0, n)
        ByteVector(bs)
      }
    }
  implicit def randomAccessFileCanSyncRead[F[_]: ContextShift: Sync]: CanSyncRead[F, RandomAccessFile] =
    new CanSyncRead[F, RandomAccessFile] {
      def syncReadByte(x: RandomAccessFile, b: Blocker) = b.delay {
        val b = x.read()
        if (b == -1) None else Some(b.toByte)
      }
      def syncReadBytes(x: RandomAccessFile, b: Blocker, n: Int) = b.delay {
        @tailrec def loop(bs: Array[Byte], o: Int, n: Int): Unit = {
          val l = x.read(bs, o, n)
          if (l > 0 && l < n) loop(bs, o + l, n - l)
        }
        val bs = Array.ofDim[Byte](n)
        loop(bs, 0, n)
        ByteVector(bs)
      }
    }

  trait CanSyncWrite[F[_], -A] {
    def syncWriteBytes(x: A, b: Blocker, bs: ByteVector): F[Unit]
    def syncFlush(x: A, b: Blocker): F[Unit]
  }
  implicit def outputStreamCanSyncWrite[F[_]: ContextShift: Sync]: CanSyncWrite[F, OutputStream] =
    new CanSyncWrite[F, OutputStream] {
      def syncWriteBytes(x: OutputStream, b: Blocker, bs: ByteVector) = b.delay(x.write(bs.toArray))
      def syncFlush(x: OutputStream, b: Blocker)                      = b.delay(x.flush())
    }
  implicit def randomAccessFileCanSyncWrite[F[_]: ContextShift: Sync]: CanSyncWrite[F, RandomAccessFile] =
    new CanSyncWrite[F, RandomAccessFile] {
      def syncWriteBytes(x: RandomAccessFile, b: Blocker, bs: ByteVector) =
        b.delay(x.write(bs.toArray))
      def syncFlush(x: RandomAccessFile, b: Blocker) = Applicative[F].unit
    }

  trait CanSyncsyncSeek[F[_], -A] {
    def syncSeek(x: A, b: Blocker, to: Long): F[Unit]
  }
  implicit def randomAccessFileCanSyncsyncSeek[F[_]: ContextShift: Sync]: CanSyncsyncSeek[F, RandomAccessFile] =
    new CanSyncsyncSeek[F, RandomAccessFile] {
      def syncSeek(x: RandomAccessFile, b: Blocker, to: Long) = b.delay(x.seek(to))
    }

  trait CanSyncsyncClose[F[_], -A] {
    def syncClose(x: A, b: Blocker): F[Unit]
  }
  implicit def closeableCanSyncsyncClose[F[_]: ContextShift: Sync]: CanSyncsyncClose[F, Closeable] =
    new CanSyncsyncClose[F, Closeable] {
      def syncClose(x: Closeable, b: Blocker) = b.delay(x.close())
    }

  def syncReadLengthBytes[F[_]: ContextShift: FlatMap: LiftIO: Sync, A](
      x: A, b: Blocker
  )(implicit CR: CanSyncRead[F, A]): F[ByteVector] =
    for {
      n <- CR
        .syncReadBytes(x, b, 4)
        .map(bs => int32.decodeValue(bs.toBitVector).require)
      bs <- CR.syncReadBytes(x, b, n)
    } yield bs
  def syncWriteLengthBytes[F[_]: ContextShift: FlatMap: Sync, A](
      x: A,
      b: Blocker,
      bs: ByteVector
  )(implicit CW: CanSyncWrite[F, A]): F[Int] =
    for {
      _ <- CW.syncWriteBytes(x, b, int32.encode(bs.length.toInt).require.toByteVector)
      _ <- CW.syncWriteBytes(x, b, bs)
    } yield 4 + bs.length.toInt
  def syncFlush[F[_]: ContextShift: Sync, A](x: A, b: Blocker)(implicit CW: CanSyncWrite[F, A]): F[Unit] = CW.syncFlush(x, b)
  def syncSeek[F[_]: ContextShift: Sync, A](x: A, b: Blocker, to: Long)(implicit CS: CanSyncsyncSeek[F, A]): F[Unit] =
    CS.syncSeek(x, b, to)
  def syncClose[F[_]: ContextShift: Sync, A](x: A, b: Blocker)(implicit CC: CanSyncsyncClose[F, A]): F[Unit] = CC.syncClose(x, b)
}
