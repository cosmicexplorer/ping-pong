package pingpong.ensime

import pingpong.ensime.PantsExportProtocol._

import org.ensime.api._
import spray.json._

object EnsimeFileGen extends App {
  val allStdin = scala.io.Source.stdin.mkString
  val jsonParsedStdin = allStdin.parseJson

  println(s"hello, world:\n${jsonParsedStdin}")

  println("hey!")
}
