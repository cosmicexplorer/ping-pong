package pingpong.parsing

import scala.util.matching.{Regex => ScalaRegex}

object Regex {
  implicit class RegexImplicits(sc: StringContext) {
    def rx(args: Any*) = new ScalaRegex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }
}
