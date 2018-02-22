package org.validoc.kleislis

import scala.concurrent.{ExecutionContext, Future}
import KleisliLangauge._
import Strings._

import scala.util.Try

trait MetricState[X] extends (Try[X] => String)

trait RecordMetricCount extends (String => Unit)

case class Metrics[X, Y](metricPrefix: String)(implicit metricState: MetricState[Y],
                                               recordMetricCount: RecordMetricCount,
                                               executionContext: ExecutionContext) extends KleisliDelegate[X, Y] {

  override def apply(service: X => Future[Y]): X => Future[Y] =
    service onComplete  (metricState andThen prefixWith(metricPrefix, ".") andThen recordMetricCount)

}


