package pingpong.ensime

import pingpong.ensime.PantsExportProtocol._

import org.ensime.api._
import spray.json._

import java.io.File

object EnsimeFileGen extends App {
  val allStdin = scala.io.Source.stdin.mkString
  val pantsExportParsed = allStdin.parseJson.convertTo[PantsExport]

  val Array(buildRoot, scalaVersion, ensimeCacheDir) = args

  val defaultJvmPlatform = pantsExportParsed.jvmPlatforms.defaultPlatform
  val javaHome = pantsExportParsed.preferredJvmDistributions(defaultJvmPlatform).strict

  val ensimeConfig = EnsimeConfig(
    name = "???",
    rootDir = RawFile(new File(buildRoot).toPath),
    cacheDir = RawFile(new File(ensimeCacheDir).toPath),
    scalaVersion = scalaVersion,
    javaHome = RawFile(new File(javaHome).toPath),
    javaSources = List(),
    projects = List(),
    compilerArgs = List(),
  )

  println(s"hello, world:\n${ensimeConfig}")

  println("hey!")
}
