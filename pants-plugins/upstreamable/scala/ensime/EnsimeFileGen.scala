package pingpong.ensime

import pingpong.ensime.PantsExportProtocol._

import ammonite.ops.Path
import org.ensime.api._
import org.ensime.config.EnsimeConfigProtocol
import org.ensime.sexp.SexpPrettyPrinter
import org.ensime.sexp.SexpWriter.ops._
import spray.json._

import java.io.File

object EnsimeFileGen extends App {
  def makeRawFile(path: String) = RawFile(new File(path).toPath)

  def validateEnsimeConfig(cfg: EnsimeConfig) = EnsimeConfigProtocol.validated(cfg)

  val Array(buildRoot, scalaVersion, ensimeCacheDir) = args

  val allStdin = scala.io.Source.stdin.mkString
  val pantsExportParsed = allStdin.parseJson.convertTo[PantsExport]

  val defaultJvmPlatform = pantsExportParsed.jvmPlatforms.defaultPlatform
  val javaHome = pantsExportParsed.preferredJvmDistributions(defaultJvmPlatform).strict

  // val projects = pantsExportParsed.targets.flatMap { case (id, target) => EnsimeProject(
  //   id = EnsimeProjectId(project = id, config = "compile"),
  //   depends = target.dependencies
  //     .map(_.map(EnsimeProjectId(project = _, config = "compile")))
  //     .getOrElse(Nil),
  //   sources = target.globs.globs.map(makeGlobMatcher(_))
  // )}

  val ensimeConfig = validateEnsimeConfig(EnsimeConfig(
    name = Path(buildRoot).last,
    rootDir = makeRawFile(buildRoot),
    cacheDir = makeRawFile(ensimeCacheDir),
    scalaVersion = scalaVersion,
    javaHome = makeRawFile(javaHome),
    javaSources = List(),
    projects = List(),
  ))

  val ensimeSexp = ensimeConfig.toSexp

  println(s"hello, world:\n${SexpPrettyPrinter(ensimeSexp)}")

  println(s"aaaaa:\n${allStdin}")

  println(s"bbbbb!!!:\n${pantsExportParsed.toJson.prettyPrint}")

  println("hey!")
}
