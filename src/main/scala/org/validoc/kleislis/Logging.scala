package org.validoc.kleislis

import java.text.MessageFormat

import scala.concurrent.{ExecutionContext, Future}
import KleisliLangauge._

import scala.util.{Failure, Success, Try}

trait Loggable[X] {
  def debug(x: X): String
  def info(x: X): String
}

object Loggable {

  implicit object ThrowableLoggable extends Loggable[Throwable] {
    override def debug(x: Throwable): String = x.getClass.getSimpleName + "/" + x.getMessage
    override def info(x: Throwable): String = debug(x)
  }

  implicit def defaultLoggable[T] = new Loggable[T] {
    override def debug(x: T): String = x.toString
    override def info(x: T): String = x.toString
  }

}

trait Logit {
  def debug(marker: String, s: String)
  def error(marker: String, s: String, t: Throwable)
}

object Logit {
  def debugString[X](x: X)(implicit loggable: Loggable[X]) = loggable.debug(x)
  def infoString[X](x: X)(implicit loggable: Loggable[X]) = loggable.info(x)

}

class LogConfig(successPattern: String, failurePattern: String)(implicit throwableLogData: Loggable[Throwable], logit: Logit) {
  def apply[Req: Loggable, Res: Loggable](marker: String)(req: Req, tryRes: Try[Res]) = tryRes match {
    case Success(res) => logit.debug(marker, MessageFormat.format(successPattern, Logit.debugString(req), Logit.debugString(res)))
    case Failure(t) => logit.error(marker, MessageFormat.format(failurePattern, Logit.infoString(req), Logit.infoString(t)), t)
  }
}

case class Logging[Req: Loggable, Res: Loggable](marker: String)(implicit logConfig: LogConfig, executionContext: ExecutionContext) extends KleisliDelegate[Req, Res] {
  override def apply(service: Req => Future[Res]): Req => Future[Res] = service onCompleteWithReq logConfig(marker)
}
