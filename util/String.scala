package pingpong.util

import scala.collection.GenTraversableOnce

object StringExt {
  implicit class StringSeqWrapper[C <: GenTraversableOnce[String]](strings: C) {
    def join(joiner: String): String = strings.reduce((acc, cur) => s"${acc}${joiner}${cur}")
  }
}
