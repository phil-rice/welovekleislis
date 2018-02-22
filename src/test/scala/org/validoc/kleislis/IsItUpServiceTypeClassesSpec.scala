package org.validoc.kleislis

import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success}

class IsItUpServiceTypeClassesSpec extends KleisliSpec {

  val isItUpRequest = IsItUpRequest("http://someHost/someUrl")
  val httpResponse200 = HttpResponse(200, "who cares", Map())
  val httpResponse201 = HttpResponse(201, "who cares", Map())
  val httpResponse400 = HttpResponse(400, "who cares", Map())
  val runtimeException = new RuntimeException("some error")

  val fromHttpResponse = implicitly[FromHttpResponse[IsItUpRequest, IsItUpResult]]
  val toHttpRequest = implicitly[ToHttpRequest[IsItUpRequest]]
  val exceptionHandler = implicitly[ExceptionHandler[IsItUpRequest, IsItUpResult]]
  val metricState = implicitly[MetricState[IsItUpResult]]

  behavior of "IsItUpRequestToHttpRequest"

  it should "make a get call to the url" in {
    toHttpRequest(isItUpRequest) shouldBe HttpRequest(Get, "http://someHost/someUrl", Map())
  }

  behavior of "FromHttpResponseForIsItUpResult"

  it should "make an IsItUpResult with the url copied" in {
    fromHttpResponse(isItUpRequest, httpResponse200).url shouldBe isItUpRequest.url
    fromHttpResponse(isItUpRequest, httpResponse201).url shouldBe isItUpRequest.url
    fromHttpResponse(isItUpRequest, httpResponse400).url shouldBe isItUpRequest.url
  }

  it should "return true IFF status code is 200" in {
    fromHttpResponse(isItUpRequest, httpResponse200).up shouldBe true
    fromHttpResponse(isItUpRequest, httpResponse201).up shouldBe false
    fromHttpResponse(isItUpRequest, httpResponse400).up shouldBe false
  }

  behavior of "ExceptionHandlerForIsItUpResult"

  it should "make an IsItUpResult copying the url from the request" in {
    exceptionHandler(isItUpRequest, runtimeException).url shouldBe isItUpRequest.url
  }

  it should "Make an IsItUpResult with 'up' being false" in {
    exceptionHandler(isItUpRequest, runtimeException).up shouldBe false
  }

  it should "return true from isdefined" in {
    exceptionHandler.isDefinedAt(isItUpRequest, runtimeException) shouldBe true
  }

  behavior of "MetricStateForIsItUpResult"

  it should "make a String 'up' when the result is up" in {
    metricState(Success(IsItUpResult("something", true))) shouldBe "up"
  }
  it should "make a String 'down' when the 'IsItUpResult is down, and no exception was thrown" in {
    metricState(Success(IsItUpResult("something", false)) )shouldBe "down"
  }
  it should "make a String 'failed' when an exception was thrown" in {
    metricState(Failure(runtimeException) )shouldBe "failed"
  }
}
