package pingpong.ensime

import pingpong.ensime.PantsExport
import pingpong.ensime.PantsExportProtocol._

import org.ensime.api._
import spray.json._

object EnsimeFileGen extends App {
  val allStdin = scala.io.Source.stdin.mkString
  val pantsExportParsed = allStdin.parseJson.convertTo[PantsExport]

  println(s"hello, world:\n${pantsExportParsed.toJson.prettyPrint}")

  println("hey!")
}
