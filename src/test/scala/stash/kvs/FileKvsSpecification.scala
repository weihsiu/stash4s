package stash.kvs

import cats._
import cats.effect._
import cats.effect.concurrent._
import cats.effect.implicits._
import cats.implicits._
import java.io._
import java.nio.file._
import org.scalacheck._
import scala.concurrent.ExecutionContext.global
import scodec.bits.ByteVector
import stash.Arbitraries._
import stash.kvs.Kvs
import stash.kvs.FileKvs
import stash.util.Pool

object FileKvsSpecification extends Properties("FileKvs") {
  import Prop.{forAll, propBoolean}
  implicit val cs = IO.contextShift(global)
  property("insert/query/remove") = forAll { (k: ByteVector, v: ByteVector) =>
    v.nonEmpty ==> {
      val test = Blocker[IO].use(
        blocker =>
          IO(Files.createTempFile("test", "kvs")) >>= (
              file =>
                Resource
                  .make(FileKvs.initFileKvs[IO](file, blocker, 3))(FileKvs.releaseFileKvs(_))
                  .use(
                    fileKvs =>
                      for {
                        _  <- fileKvs.insert(k, v)
                        r1 <- fileKvs.query(k)
                        _  <- fileKvs.remove(k)
                        r2 <- fileKvs.query(k)
                      } yield r1 == Some(v) && r2 == None
                  )
            )
      )
      test.unsafeRunSync()
    }
  }
  property("keys and values persist after release") = forAll { (k: ByteVector, v: ByteVector) =>
    v.nonEmpty ==> {
      val test = Blocker[IO].use(
        blocker =>
          IO(Files.createTempFile("test", "kvs")) >>= (
              file =>
                for {
                  r1 <- Resource
                    .make(FileKvs.initFileKvs[IO](file, blocker, 3))(FileKvs.releaseFileKvs(_))
                    .use(
                      fileKvs =>
                        for {
                          _ <- fileKvs.insert(k, v)
                          r <- fileKvs.query(k)
                        } yield r
                    )
                  r2 <- Resource
                    .make(FileKvs.initFileKvs[IO](file, blocker, 3))(FileKvs.releaseFileKvs(_))
                    .use(
                      fileKvs =>
                        for {
                          r <- fileKvs.query(k)
                          _ <- fileKvs.remove(k)
                        } yield r
                    )
                } yield r1 == v.some && r1 == r2
            )
      )
      test.unsafeRunSync()
    }
  }
}
