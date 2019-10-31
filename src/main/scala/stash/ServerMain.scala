package stash

import cats.effect._
import java.nio.file.Paths
import stash.KvsServer._
import stash.Env._
import stash.RIOs._
import stash.kvs.FileKvs

object ServerMain extends IOApp {
  def run(args: List[String]): IO[ExitCode] = Blocker[IO].use(
    blocker =>
      Resource
        .make(FileKvs.initFileKvs[IO](Paths.get("data/stash.kvs"), blocker, 3))(FileKvs.releaseFileKvs(_))
        .use(
          fileKvs => ??? //runRIO(ServerApp(fileKvs), runServer)
        )
  )
  def runServer(implicit KS: KvsServer[RIO[ServerApp[IO], *]]): RIO[ServerApp[IO], Unit] = {
    KS.serve("localhost", 8000)
  }
}