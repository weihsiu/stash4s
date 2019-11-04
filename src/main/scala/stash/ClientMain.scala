package stash

import cats.effect._
import scodec.bits._
import stash.kvs.ClientKvs._

object ClientMain extends IOApp {
  def run(args: List[String]): IO[ExitCode] = for {
    clientKvs <- initClientKvs[IO]("localhost", 8000)
    _ <- clientKvs.insert(ByteVector(1, 2, 3), ByteVector(4, 5, 6))
    Some(v) <- clientKvs.query(ByteVector(1, 2, 3))
    _ = assert(v == ByteVector(4, 5, 6))
    _ <- clientKvs.remove(ByteVector(1, 2, 3))
    r <- clientKvs.query(ByteVector(1, 2, 3))
    _ = assert(r.isEmpty)
  } yield ExitCode.Success
}