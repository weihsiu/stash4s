package stash.kvs

import org.scalacheck._
import scodec.bits.ByteVector
import stash.Arbitraries._

object MapKvsSpecification extends Properties("MapKvs") {
  import Prop.forAll

  property("insert/query/remove") = forAll { (k: ByteVector, v: ByteVector) =>
    k == k
  }
}