import cats._
import cats.effect._
import cats.implicits._

object GenericTest extends App {
  implicit val list2Option: List ~> Option = Lambda[List ~> Option](_.headOption)
  def test[F[_], G[_]: Functor, A](f: F[A])(implicit F2G: F ~> G): G[(A, A)] = F2G(f).map(x => (x, x))
  val r = test[List, Option, Int](List(1))
  println(r)
}