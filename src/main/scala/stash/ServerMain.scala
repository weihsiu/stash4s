package stash

import cats._
import cats.data._
import cats.effect._
import cats.effect.concurrent._
import cats.effect.implicits._
import cats.implicits._
import cats.mtl._
import cats.mtl.instances.all._
import cats.mtl.syntax.all._
import com.olegpy.meow.effects._
import com.olegpy.meow.hierarchy._
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
          fileKvs => Ref.unsafe[IO, ServerApp[IO]](ServerApp(fileKvs)).runAsk { implicit askInstance =>
            implicit val liftIO = implicitly[LiftIO[RIO[ServerApp[IO], *]]]
            val server: KvsServer[RIO[ServerApp[IO], *]] = kvsServer[RIO[ServerApp[IO], *], IO, ServerApp[IO]]
            runRIO(ServerApp(fileKvs), runServer(server).map(_ => ExitCode.Success)).run(ServerApp(fileKvs))
          }
        )
  )
  def runServer(implicit KS: KvsServer[RIO[ServerApp[IO], *]]): RIO[ServerApp[IO], Unit] = {
    KS.serve("localhost", 8000)
  }
}