package org.validoc.kleislis

import scala.concurrent.{ExecutionContext, Future}
import KleisliLangauge._

trait ToHttpRequest[Req] extends (Req => HttpRequest)

trait FromHttpResponse[Req, Res] extends ((Req, HttpResponse) => Res)


case class Objectify[X, Y](implicit toHttpRequest: ToHttpRequest[X],
                           fromHttpResponse: FromHttpResponse[X, Y],
                           executionContext: ExecutionContext) extends KleisliTransformer[HttpRequest, HttpResponse, X, Y] {

  override def apply(httpService: HttpRequest => Future[HttpResponse]): X => Future[Y] =
    toHttpRequest andThen httpService andThenWithReq fromHttpResponse

}


