package org.validoc.kleislis

object Strings {

  def prefixWith(prefix: String*)(string: String) = (prefix :+ string).mkString("")
}
