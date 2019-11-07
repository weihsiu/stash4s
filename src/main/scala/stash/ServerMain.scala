package stash

import cats._
import cats.data._
import cats.effect._
import cats.effect.concurrent._
import cats.effect.implicits._
import cats.implicits._
import cats.mtl._
import cats.mtl.instances.all._
import com.olegpy.meow.effects._
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
          fileKvs => for {
            env <- Ref.of[IO, ServerApp[IO]](ServerApp(fileKvs))
            r <- env.runAsk { implicit askInstance =>
              val server: KvsServer[RIO[ServerApp[IO], *]] = kvsServer[RIO[ServerApp[IO], *], IO, ServerApp[IO]]
              askInstance.ask >>= (runServer(server).run(_))
            }
          } yield r
        )
  ).as(ExitCode.Success)
  def runServer(server: KvsServer[RIO[ServerApp[IO], *]]): RIO[ServerApp[IO], Unit] = {
    server.serve("localhost", 8000)
  }
}