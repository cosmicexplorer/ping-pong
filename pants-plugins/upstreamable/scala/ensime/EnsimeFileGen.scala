package pingpong.ensime

import pingpong.ensime.PantsExportProtocol._

import ammonite.ops.{Path, RelPath}
import org.ensime.api._
import org.ensime.config.EnsimeConfigProtocol
import org.ensime.sexp.{SexpReader, SexpWriter}
import org.ensime.sexp.SexpPrettyPrinter
import org.ensime.sexp.SexpWriter.ops._
import scalaz.deriving
import spray.json._

import scala.collection.TraversableOnce._
import scala.sys

import java.io.File
import java.io.PrintWriter

// @deriving(SexpReader, SexpWriter)
// final case class ExpandedEnsimeConfig(
//   rootDir: RawFile,
//   cacheDir: RawFile,
//   javaHome: RawFile,
//   name: String,
//   scalaVersion: String,
//   javaSources: List[RawFile],
//   projects: List[EnsimeProject],
//   ensimeServerVersion: String,
//   ensimeServerJars: List[RawFile],
//   scalaCompilerJars:
// )

object EnsimeFileGen extends App {
  def makeRawFile(path: String) = RawFile(new File(path).toPath)

  // TODO: use this (but it canonicalizes our symlinks, which is sad)
  def validateEnsimeConfig(cfg: EnsimeConfig) = EnsimeConfigProtocol.validated(cfg)

  val Array(buildRoot, scalaVersion, ensimeCacheDir, zincCompileDir, outputFile) = args

  val buildRootPath = Path(buildRoot)
  val zincBasePath = Path(zincCompileDir)
  val outputFilePath = Path(outputFile)

  val allStdin = scala.io.Source.stdin.mkString
  val pantsExportParsed = allStdin.parseJson.convertTo[PantsExport]

  val defaultJvmPlatform = pantsExportParsed.jvmPlatforms.defaultPlatform
  val javaHome = pantsExportParsed.preferredJvmDistributions(defaultJvmPlatform).strict

  // FIXME: use safe shlex methods here!
  val scalacEnvArgs = sys.env("PANTS_SCALAC_ARGS").split(" ")
  val javacEnvArgs = sys.env("PANTS_JAVAC_ARGS").split(" ")

  val sourceTargets = pantsExportParsed.targets.filter(_._2.targetType == "scala_library")
  val sourceTargetSet = sourceTargets.map(_._1).toSet

  val projects: List[EnsimeProject] = sourceTargets
    .map { case (id, target) =>

      val curTargetPath = zincBasePath / "current" / RelPath(target.id) / "current" / "classes"
      val dependentJars = target.libraries.flatMap { coord =>
        pantsExportParsed.libraries(coord)
          .get(target.scope)
          .map(_.split(":").toSeq)
          .getOrElse(Seq())
      }

      val dependentTargets = target.dependencies
        .map { deps => deps
          .filter(sourceTargetSet)
          .map(dep => EnsimeProjectId(project = dep, config = "compile"))
          .toList
        }
        .getOrElse(Nil)

      EnsimeProject(
        // TODO: make "compile" into target.scope!
        id = EnsimeProjectId(project = id, config = "compile"),
        depends = dependentTargets,
        sources = target.sources
          .map(srcRelPath => buildRootPath / RelPath(srcRelPath))
          .map(srcPath => makeRawFile(srcPath.toString))
          .toList,
        targets = List(makeRawFile(curTargetPath.toString)),
        scalacOptions = scalacEnvArgs.toList,
        javacOptions = javacEnvArgs.toList,
        libraryJars = dependentJars.map(jar => makeRawFile(jar.toString)).toList,
        // TODO: turn on sources and libraries in coursier resolve!
        librarySources = Nil,
        libraryDocs = Nil,
      )
    }
    .toList

  val ensimeConfig = EnsimeConfig(
    name = buildRootPath.last,
    rootDir = makeRawFile(buildRoot),
    cacheDir = makeRawFile(ensimeCacheDir),
    scalaVersion = scalaVersion,
    javaHome = makeRawFile(javaHome),
    javaSources = Nil,
    projects = projects,
  )

  val outputFileWriter = new PrintWriter(outputFilePath.toIO)

  val ensimeSexp = ensimeConfig.toSexp

  outputFileWriter.println(SexpPrettyPrinter(ensimeConfig.toSexp))

  outputFileWriter.close()

  println("hey!")

  println(s"aaaaa:\n${allStdin}")

  println(s"bbbbb!!!:\n${pantsExportParsed.toJson.prettyPrint}")
}
