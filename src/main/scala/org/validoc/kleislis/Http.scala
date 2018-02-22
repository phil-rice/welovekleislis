package org.validoc.kleislis

sealed trait HttpVerb
case object Get extends HttpVerb
case object Post extends HttpVerb

case class HttpRequest (verb: HttpVerb, url: String, headers: Map[String, String])
case class HttpResponse (statusCode: Int, body: String, headers: Map[String, String])
