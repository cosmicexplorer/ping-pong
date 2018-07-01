package pingpong.ensime

import pingpong.ensime.PantsExportProtocol._

import org.ensime.api._
import org.ensime.config.EnsimeConfigProtocol
import org.ensime.sexp.SexpPrettyPrinter
import org.ensime.sexp.SexpWriter.ops._
import spray.json._

import java.io.File

object EnsimeFileGen extends App {
  def makeRawFile(path: String) = RawFile(new File(path).toPath)

  val Array(buildRoot, scalaVersion, ensimeCacheDir) = args

  val allStdin = scala.io.Source.stdin.mkString
  val pantsExportParsed = allStdin.parseJson.convertTo[PantsExport]

  val defaultJvmPlatform = pantsExportParsed.jvmPlatforms.defaultPlatform
  val javaHome = pantsExportParsed.preferredJvmDistributions(defaultJvmPlatform).strict

  val ensimeConfig = EnsimeConfigProtocol.validated(EnsimeConfig(
    name = "???",
    rootDir = makeRawFile(buildRoot),
    cacheDir = makeRawFile(ensimeCacheDir),
    scalaVersion = scalaVersion,
    javaHome = makeRawFile(javaHome),
    javaSources = List(),
    projects = List(),
  ))

  val ensimeSexp = ensimeConfig.toSexp

  println(s"hello, world:\n${SexpPrettyPrinter(ensimeSexp)}")

  println("hey!")
}
