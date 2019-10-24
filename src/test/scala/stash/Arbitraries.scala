package stash

import org.scalacheck._
import org.scalacheck.Arbitrary._
import scodec.bits.ByteVector

object Arbitraries {
  implicit lazy val arbByteVector: Arbitrary[ByteVector] =
    Arbitrary(Gen.containerOf[Array, Byte](arbitrary[Byte]).map(ByteVector(_)))

  // implicit lazy val shrinkByteVector: Shrink[ByteVector] = Shrink { b =>
  //   shrink(b.toArray) map { s => ByteVector(s) }
  // }
}