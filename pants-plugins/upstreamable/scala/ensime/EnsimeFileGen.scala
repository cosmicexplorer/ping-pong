package pingpong.ensime

import pingpong.ensime.PantsExportProtocol._

import ammonite.ops.{Path, RelPath}
import org.ensime.api._
import org.ensime.config.EnsimeConfigProtocol
import org.ensime.sexp.SexpPrettyPrinter
import org.ensime.sexp.SexpWriter.ops._
import spray.json._

import scala.collection.TraversableOnce._

import java.io.File

object EnsimeFileGen extends App {
  def makeRawFile(path: String) = RawFile(new File(path).toPath)

  def validateEnsimeConfig(cfg: EnsimeConfig) = EnsimeConfigProtocol.validated(cfg)

  val Array(buildRoot, scalaVersion, ensimeCacheDir) = args

  val buildRootPath = Path(buildRoot)

  val allStdin = scala.io.Source.stdin.mkString
  val pantsExportParsed = allStdin.parseJson.convertTo[PantsExport]

  val defaultJvmPlatform = pantsExportParsed.jvmPlatforms.defaultPlatform
  val javaHome = pantsExportParsed.preferredJvmDistributions(defaultJvmPlatform).strict

  val projects: List[EnsimeProject] = pantsExportParsed.targets.map {
      case (id, target) => EnsimeProject(
        id = EnsimeProjectId(project = id, config = "compile"),
        depends = target.dependencies
          .map(_.map(dep => EnsimeProjectId(project = dep, config = "compile")).toList)
          .getOrElse(Nil),
        sources = target.sources
          .map(srcRelPath => buildRootPath / RelPath(srcRelPath))
          .map(srcPath => makeRawFile(srcPath.toString))
          .toList,
        targets = Nil,
        scalacOptions = Nil,
        javacOptions = Nil,
        libraryJars = Nil,
        librarySources = Nil,
        libraryDocs = Nil,
      )}.toList

  val ensimeConfig = validateEnsimeConfig(EnsimeConfig(
    name = buildRootPath.last,
    rootDir = makeRawFile(buildRoot),
    cacheDir = makeRawFile(ensimeCacheDir),
    scalaVersion = scalaVersion,
    javaHome = makeRawFile(javaHome),
    javaSources = Nil,
    projects = projects,
  ))

  val ensimeSexp = ensimeConfig.toSexp

  println(s"hello, world:\n${SexpPrettyPrinter(ensimeSexp)}")

  println(s"aaaaa:\n${allStdin}")

  println(s"bbbbb!!!:\n${pantsExportParsed.toJson.prettyPrint}")

  println("hey!")
}
