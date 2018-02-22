package org.validoc.kleislis

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

trait KleisliSpec extends FlatSpec with Matchers {

  implicit class FuturePimper[T](f: Future[T]) {
    def futureValue: T = Await.result(f, 5 seconds)
    def futureException: RuntimeException = intercept[RuntimeException](futureValue)
  }

}
case class MockFunction[X, Y](expected: X, returns: Y) extends (X => Y) with Matchers {
  override def apply(v1: X): Y = {
    v1 shouldBe expected
    returns
  }

}

case class MockFunction2[X, Y, Z](expected1: X, expected2: Y, returns: Z) extends ((X, Y) => Z) with Matchers {
  override def apply(v1: X, v2: Y): Z = {
    v1 shouldBe expected1
    v2 shouldBe expected2
    returns
  }

}
