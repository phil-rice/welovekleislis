package org.validoc.kleislis

import org.validoc.kleislis.KleisliLangauge._

import scala.concurrent.{ExecutionContext, Future}

case class IsItUpRequest(url: String)

object IsItUpRequest {

  implicit object IsItUpRequestToHttpRequest extends ToHttpRequest[IsItUpRequest] {
    override def apply(req: IsItUpRequest): HttpRequest = HttpRequest(Get, req.url, Map())
  }

}

case class IsItUpResult(url: String, up: Boolean)

object IsItUpResult {

  implicit object FromHttpResponseForIsItUpResult extends FromHttpResponse[IsItUpRequest, IsItUpResult] {
    override def apply(v1: IsItUpRequest, v2: HttpResponse): IsItUpResult = IsItUpResult(v1.url, v2.statusCode == 200)
  }

  implicit object ExceptionHandlerForIsItUpResult extends ExceptionHandler[IsItUpRequest, IsItUpResult] {
    override def apply(req: IsItUpRequest, v2: Throwable): IsItUpResult = IsItUpResult(req.url, false)
    override def isDefinedAt(req: IsItUpRequest, throwable: Throwable): Boolean = true
  }

  implicit object MetricStateForIsItUpResult extends MetricState[IsItUpResult] {
    val lookup = Map(false -> "down", true -> "up")
    override def apply(isItUpResult: IsItUpResult): String = lookup(isItUpResult.up)
  }

}

class IsItUpService(http: HttpRequest => Future[HttpResponse])(implicit toHttpRequest: ToHttpRequest[IsItUpRequest],
                                                               fromHttpResponse: FromHttpResponse[IsItUpRequest, IsItUpResult],
                                                               exceptionHandler: ExceptionHandler[IsItUpRequest, IsItUpResult],
                                                               metricState: MetricState[IsItUpResult],
                                                               recordMetricCount: RecordMetricCount, executionContext: ExecutionContext) {
  def service: IsItUpRequest => Future[IsItUpResult] = http |+| Objectify[IsItUpRequest, IsItUpResult]() |+| Metrics("isItUp")

}
