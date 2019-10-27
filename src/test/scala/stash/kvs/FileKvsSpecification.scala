package stash.kvs

import cats._
import cats.effect._
import cats.effect.concurrent._
import cats.effect.implicits._
import cats.implicits._
import java.io._
import org.scalacheck._
import scala.concurrent.ExecutionContext.global
import scodec.bits.ByteVector
import stash.Arbitraries._
import stash.kvs.Kvs
import stash.kvs.FileKvs
import stash.util.Pool

object FileKvsSpecification extends Properties("FileKvs") {
  import Prop.{forAll, propBoolean}
  property("insert/query/remove") = forAll { (k: ByteVector, v: ByteVector) => v.nonEmpty ==> {
    implicit val cs = IO.contextShift(global)
    val test = for {
      offsets <- Ref.of[IO, Map[ByteVector, Long]](Map.empty)
      output <- MVar.of[IO, (Long, OutputStream)]((0, new FileOutputStream("data/test")))
      inputs <- Pool.of[IO, RandomAccessFile](List.fill(3)(new RandomAccessFile("data/test", "r")))
      fileKvs = FileKvs(offsets, output, inputs)
      _ <- fileKvs.insert(k, v)
      _ = println("inserted")
      r1 <- fileKvs.query(k)
      _ = println(s"r1 = $r1")
      _ <- fileKvs.remove(k)
      r2 <- fileKvs.query(k)
    } yield r1 == Some(v) && r2 == None
    test.unsafeRunSync()
  }}
}