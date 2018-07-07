package pingpong.parsing

import scala.util.matching.{Regex => ScalaRegex}

object Regex {
  // FIXME: Any $ must be doubled (i.e. $$) -- we shouldn't need to do this.
  implicit class RegexImplicits(sc: StringContext) {
    def rx(args: Any*) = new ScalaRegex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }
}
