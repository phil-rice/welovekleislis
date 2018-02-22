package org.validoc.kleislis

import java.io.{ByteArrayOutputStream, PrintStream}

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future
import KleisliLangauge._
import org.scalatest.mockito.MockitoSugar

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Try}
import org.mockito.Mockito._

class KleisliLanguageSpec extends KleisliSpec with MockitoSugar {
  val runtimeException = new RuntimeException

  behavior of "KleisliLanguage.andThenK"

  it should "use flatMap to chain kleislis no exceptions" in {
    val k = MockFunction("a", Future.successful(1)) andThenK MockFunction(1, Future.successful("b"))
    k("a").futureValue shouldBe "b"
  }

  it should "useflatMap to chain kleislis exception in first" in {
    val k = MockFunction("a", Future.failed(runtimeException)) andThenK MockFunction(1, Future.successful("b"))
    k("a").futureException shouldBe runtimeException
  }
  it should "use flatMap to chain kleislis exception in second" in {
    val k = MockFunction("a", Future.successful(1)) andThenK MockFunction(1, Future.failed(runtimeException))
    k("a").futureException shouldBe runtimeException
  }
  behavior of "KleisliLanguage.andThenF"

  it should "use flatMap to chain kleislis no exceptions" in {
    val k = MockFunction("a", Future.successful(1)) andThenF MockFunction(1, "b")
    k("a").futureValue shouldBe "b"
  }
  it should "use map to chain kleislis exception in first" in {
    val k: String => Future[String] = MockFunction("a", Future.failed(runtimeException)) andThenF MockFunction(1, "b")
    k("a").futureException shouldBe runtimeException
  }
  it should "use  map to chain kleislis exception in second" in {
    val k = MockFunction("a", Future.successful(1)) andThenF { x: Int => throw runtimeException }
    k("a").futureException shouldBe runtimeException
  }

  behavior of "KleisliLangauge.andThenWithReq"

  it should "use map and pass the original request and the result of the kleisli to the second" in {
    val k = MockFunction("a", Future.successful(1)) andThenWithReq MockFunction2("a", 1, "b")
    k("a").futureValue shouldBe "b"
  }
  it should "use map and return an exception thrown in the first" in {
    val k = MockFunction("a", Future.failed(runtimeException)) andThenWithReq MockFunction2(1, "a", "b")
    k("a").futureException shouldBe runtimeException
  }
  it should "use map and return an exception thrown in the second" in {
    val k = MockFunction("a", Future.successful(1)) andThenWithReq { (original: String, x: Int) => throw runtimeException }
    k("a").futureException shouldBe runtimeException
  }

  behavior of "debugFn"

  def checkPrintln[X](expected: String)(block: => X): X = {
    val stream = new ByteArrayOutputStream()
    val result = Console.withOut(new PrintStream(stream))(block)
    stream.toString("UTF-8").trim shouldBe expected
    result

  }

  it should "println the o/p" in {
    val f = MockFunction(1, 2) andThen debugFn("result:")
    checkPrintln("result: 2")(f(1)) shouldBe 2
  }

  //  it should "call the passed in function with the try of the result of the kleisli" in {
  //    val fn = mock[Try[Int] => Unit]
  //    val k = MockFunction("a", Future.successful(1)) onSuccess  fn
  //    k("a").futureValue
  //    when(fn.apply(Success(1)))
  //  }

}
