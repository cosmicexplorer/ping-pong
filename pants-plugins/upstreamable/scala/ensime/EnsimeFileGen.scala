package pingpong.ensime

import pingpong.ensime.PantsExportProtocol._

import org.ensime.api._
import org.ensime.config.EnsimeConfigProtocol._
import org.ensime.config.richconfig._
import org.ensime.sexp._
import org.ensime.sexp.SexpFormat._
import spray.json._

import java.io.File

object EnsimeFileGen extends App {
  val Array(buildRoot, scalaVersion, ensimeCacheDir) = args

  val allStdin = scala.io.Source.stdin.mkString
  val pantsExportParsed = allStdin.parseJson.convertTo[PantsExport]

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

  val ensimeSexp = SexpWriter(ensimeConfig)

  println(s"hello, world:\n${ensimeSexp}")

  println("hey!")
}
