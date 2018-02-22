package org.validoc.kleislis

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Failed, FlatSpec, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import org.mockito.Mockito._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

class IsItUpServiceSpec extends KleisliSpec with MockitoSugar {
  val isItUpRequest = IsItUpRequest("http://someHost/someUrl")
  val httpRequest = HttpRequest(Get, "an unimportant url", Map())
  val httpResponse200 = HttpResponse(200, "who cares", Map())
  val httpResponse201 = HttpResponse(201, "who cares", Map())
  val httpResponse400 = HttpResponse(400, "who cares", Map())
  val runtimeException = new RuntimeException("some error")
  val isItUpresultTrue = IsItUpResult("someUrl", true)
  val isItUpresultUnique = IsItUpResult("someUniqueUrl", true)


  behavior of "IsItUpService"

  def setup(httpResponse: Try[HttpResponse], isItUpResult: IsItUpResult)
           (fn: (IsItUpRequest => Future[IsItUpResult], HttpRequest => Future[HttpResponse], ToHttpRequest[IsItUpRequest], FromHttpResponse[IsItUpRequest, IsItUpResult], MetricState[IsItUpResult], ExceptionHandler[IsItUpRequest, IsItUpResult], RecordMetricCount) => Unit) = {
    implicit val exceptionHandler = mock[ExceptionHandler[IsItUpRequest, IsItUpResult]]
    implicit val metricState = new MockFunction(isItUpResult, "someMetricState") with MetricState[IsItUpResult]
    implicit val recordMetricCount = mock[RecordMetricCount]
    implicit val httpService = MockFunction(httpRequest, httpResponse.fold(Future.failed, Future.successful))
    implicit val toHttpRequest = new MockFunction(isItUpRequest, httpRequest) with ToHttpRequest[IsItUpRequest]
    implicit val fromHttpResponse = new MockFunction2(isItUpRequest, httpResponse.fold(_ => null, x => x), isItUpResult) with FromHttpResponse[IsItUpRequest, IsItUpResult]
    fn(new IsItUpService(httpService).service, httpService, toHttpRequest, fromHttpResponse, metricState, exceptionHandler, recordMetricCount)
  }


  it should "have a rosy view that chains the toHttpRequest to the httpService to the fromHttpResponse" in {
    setup(Success(httpResponse200), isItUpresultTrue) { (isItUpService, http, toHttpRequest, fromHttpResponse, metricState, exceptionHandler, recordMetricCount) =>
      isItUpService(isItUpRequest).futureValue shouldBe isItUpresultTrue
    }
    setup(Success(httpResponse200), isItUpresultUnique) { (isItUpService, http, toHttpRequest, fromHttpResponse, metricState, exceptionHandler, recordMetricCount) =>
      isItUpService(isItUpRequest).futureValue shouldBe isItUpresultUnique
    }
  }

  it should "use the exceptionHandler if there is an exception, if the 'isDefinedAt' is true" in {
    setup(Failure(runtimeException), isItUpresultUnique) { (isItUpService, http, toHttpRequest, fromHttpResponse, metricState, exceptionHandler, recordMetricCount) =>
      when(exceptionHandler.isDefinedAt(isItUpRequest, runtimeException)) thenReturn true
      when(exceptionHandler.apply(isItUpRequest, runtimeException)) thenReturn isItUpresultUnique
      isItUpService(isItUpRequest).futureValue shouldBe isItUpresultUnique
    }
  }
  it should "use not use the exceptionHandler if there is an exception, if the 'isDefinedAt' is false" in {
    setup(Failure(runtimeException), isItUpresultUnique) { (isItUpService, http, toHttpRequest, fromHttpResponse, metricState, exceptionHandler, recordMetricCount) =>
      when(exceptionHandler.isDefinedAt(isItUpRequest, runtimeException)) thenReturn false
      isItUpService(isItUpRequest).futureException shouldBe runtimeException
    }
  }

  it should "report the metrics based on the metricstate" in {
    setup(Success(httpResponse200), isItUpresultTrue) { (isItUpService, http, toHttpRequest, fromHttpResponse, metricState, exceptionHandler, recordMetricCount) =>
      isItUpService(isItUpRequest).futureValue
      verify(recordMetricCount, times(1)).apply("isItUp.someMetricState")
    }
  }


}