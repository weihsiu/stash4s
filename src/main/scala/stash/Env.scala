package stash

import monocle.macros._
import stash.kvs.FileKvs

object Env {
  @Lenses case class ServerApp[F[_]](
    fileKvs: FileKvs[F]
  )
}