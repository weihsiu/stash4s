package stash

import cats._
import cats.data._
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import cats.mtl._
import cats.mtl.instances.all._
import java.nio.file.Paths
import stash.KvsServer._
import stash.Env._
import stash.RIOs._
import stash.kvs._

object ServerMain extends IOApp {
  def run(args: List[String]): IO[ExitCode] = Blocker[IO].use(
    blocker =>
      Resource
        .make(FileKvs.initFileKvs[IO](Paths.get("data/stash.kvs"), blocker, 3))(FileKvs.releaseFileKvs(_))
        .use(
          fileKvs => runRIO(ServerApp(fileKvs), runServer)
        )
  )
  implicitly[ApplicativeAsk[IO, String]]
  val server: KvsServer[RIO[ServerApp[IO], *]] = kvsServer[RIO[ServerApp[IO], *], IO, ServerApp[IO]]
  def runServer(implicit KS: KvsServer[RIO[ServerApp[IO], *]]): RIO[ServerApp[IO], Unit] = {
    KS.serve("localhost", 8000)
  }
}