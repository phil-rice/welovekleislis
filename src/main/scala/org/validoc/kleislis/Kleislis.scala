package org.validoc.kleislis

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait ErrorHandler[X, Y] extends (X => Option[Y])

trait ExceptionHandler[Req, Res] extends ((Req, Throwable) => Res) {
  def isDefinedAt(req: Req, throwable: Throwable): Boolean

}

trait KleisliTransformer[Req, Res, NewReq, NewRes] extends ((Req => Future[Res]) => (NewReq => Future[NewRes]))

trait KleisliDelegate[Req, Res] extends KleisliTransformer[Req, Res, Req, Res]

trait KleisliLangauge {

  def debugFn[X](msg: String): X => X = { x: X => println(s"$msg $x"); x }

  implicit class KleisliPimper[Req, Res](k1: Req => Future[Res])(implicit executionContext: ExecutionContext) {
    def andThenK[Res2](k2: Res => Future[Res2]): Req => Future[Res2] = { x: Req => k1(x).flatMap(k2) }
    def andThenF[Res2](k2: Res => Res2): Req => Future[Res2] = { req: Req => k1(req).map(k2) }
    def andThenWithReq[Res2](k2: (Req, Res) => Res2): Req => Future[Res2] = { req: Req => k1(req).map(k2(req, _)) }
    def onComplete(fn: Try[Res] => Unit): Req => Future[Res] = { req: Req => k1(req).transform { tryRes => fn(tryRes); tryRes } }
    def onCompleteWithReq(fn: (Req, Try[Res]) => Unit): Req => Future[Res] = { req: Req => k1(req).transform { tryRes => fn(req, tryRes); tryRes } }
    def debug(msg: String): Req => Future[Res] = { req: Req => k1(req).map { res => println(msg + res); res } }

    def recoverFromException(exceptionHandler: ExceptionHandler[Req, Res]): Req => Future[Res] = { req: Req =>
      k1(req).recover[Res] { case e if exceptionHandler.isDefinedAt(req, e) => exceptionHandler(req, e) }
    }

    def |+|[NewReq, NewRes](fn: KleisliTransformer[Req, Res, NewReq, NewRes]): NewReq => Future[NewRes] = fn(k1)
  }

}

object KleisliLangauge extends KleisliLangauge