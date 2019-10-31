package stash

import monocle._
import monocle.macros._
import stash.kvs._
import stash.kvs.FileKvs

object Env {
  case class ServerApp[F[_]](
    fileKvs: FileKvs[F]
  )
  implicit def hasFileKvs[F[_]]: HasFileKvs[F, ServerApp[F]] = new HasFileKvs[F, ServerApp[F]] {
    def fileKvsL: Lens[ServerApp[F], FileKvs[F]] = GenLens[ServerApp[F]](_.fileKvs)
  }
}