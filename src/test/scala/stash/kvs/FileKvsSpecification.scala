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
  property("insert/query/remove") = forAll { (k: ByteVector, v: ByteVector) =>
    v.nonEmpty ==> {
      implicit val cs = IO.contextShift(global)
      val test = Resource
        .make(FileKvs.initFileKvs[IO]("data/test.kvs", 3))(FileKvs.releaseFileKvs(_))
        .use(
          fileKvs =>
            for {
              _  <- fileKvs.insert(k, v)
              r1 <- fileKvs.query(k)
              _  <- fileKvs.remove(k)
              r2 <- fileKvs.query(k)
            } yield r1 == Some(v) && r2 == None
        )
      test.unsafeRunSync()
    }
  }
}
