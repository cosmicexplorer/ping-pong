package pingpong.ensime

import pingpong.ensime.ExpandedEnsimeSexpProtocol._
import pingpong.ensime.PantsExportProtocol._

import ammonite.ops._
import org.ensime.api._
import org.ensime.sexp._
import spray.json._

import scala.collection.TraversableOnce._
import scala.sys

import java.io.PrintWriter

object EnsimeFileGen extends App {
  def makeRawFile(path: String) = RawFile(Path(path).toNIO)

  val Array(
    buildRoot,
    scalaVersion,
    ensimeCacheDir,
    zincCompileDir,
    outputFile,
    ensimeServerVersion) = args

  val buildRootPath = Path(buildRoot)
  val zincBasePath = Path(zincCompileDir)
  val outputFilePath = Path(outputFile)

  val allStdin = scala.io.Source.stdin.mkString
  val pantsExportParsed = allStdin.parseJson.convertTo[PantsExport]

  val defaultJvmPlatform = pantsExportParsed.jvmPlatforms.defaultPlatform
  val javaHome = pantsExportParsed.preferredJvmDistributions(defaultJvmPlatform).strict

  val scalacEnvArgs = sys.env("SCALAC_ARGS").parseJson.convertTo[Seq[String]]
  val javacEnvArgs = sys.env("JAVAC_ARGS").parseJson.convertTo[Seq[String]]

  val ensimeServerJars = sys.env("ENSIME_SERVER_JARS_CLASSPATH").split(":").toSeq
  val scalaCompilerJars = sys.env("SCALA_COMPILER_JARS_CLASSPATH").split(":").toSeq

  val sourceTargetTypes = Set("scala_library", "java_library", "junit_tests")

  val sourceTargets = pantsExportParsed.targets.filter { case (_, target) =>
    sourceTargetTypes(target.targetType)
  }
  // Refer to dependencies by their `id`, not by `depName` (which is a pants target spec -- not
  // allowed).
  val sourceTargetMap = sourceTargets
    .map { case (depName, target) => (depName, (target.scope, target.id)) }
    .toMap

  val projects: Seq[EnsimeProject] = sourceTargets
    .map { case (_, target) =>

      val curTargetPath = zincBasePath / "current" / RelPath(target.id) / "current" / "classes"
      val dependentJars = target.libraries.flatMap { coord =>
        pantsExportParsed.libraries.get(coord)
          .getOrElse(Seq())
          // Get libraries in all "scopes". "Scopes" here refers to a maven classifier -- NOT the
          // same as `target.scope`!
          .flatMap(_._2.split(":").toSeq)
      }

      val dependentTargets = target.dependencies
        .map(_.flatMap(sourceTargetMap.get(_))
          .map { case (scope, id) => EnsimeProjectId(project = id, config = scope) })
        .getOrElse(Seq())

      val sources = target.sources.getOrElse(Seq())
        .map(srcRelPath => buildRootPath / RelPath(srcRelPath))
        .map(_.toIO.getParent)
        .map(makeRawFile)
        .toSet

      EnsimeProject(
        // NB: There are certain restrictions on the string provided for the "project" field here
        // that are a result of translating it directly into an Akka address.
        id = EnsimeProjectId(project = target.id, config = target.scope),
        depends = dependentTargets.toList,
        sources = sources,
        targets = Set(makeRawFile(curTargetPath.toString)),
        scalacOptions = scalacEnvArgs.toList,
        javacOptions = javacEnvArgs.toList,
        libraryJars = dependentJars.map(jar => makeRawFile(jar.toString)).toList,
        // TODO: turn on sources and libraries in coursier resolve!
        librarySources = Nil,
        libraryDocs = Nil)
    }
    .toSeq

  val ensimeConfig = ExpandedEnsimeConfig(
    rootDir = makeRawFile(buildRoot),
    cacheDir = makeRawFile(ensimeCacheDir),
    javaHome = makeRawFile(javaHome),
    name = buildRootPath.last,
    scalaVersion = scalaVersion,
    compilerArgs = Seq(),
    javaSources = Seq(),
    projects = projects,
    ensimeServerJars = ensimeServerJars.map(makeRawFile),
    ensimeServerVersion = ensimeServerVersion,
    scalaCompilerJars = scalaCompilerJars.map(makeRawFile))

  val outputFileWriter = new PrintWriter(outputFilePath.toIO)

  val ensimeSexp = ensimeConfig.toSexp

  outputFileWriter.println(SexpPrettyPrinter(ensimeSexp))

  outputFileWriter.close()

  println("hey!")

  println(s"aaaaa:\n${allStdin}")

  println(s"bbbbb!!!:\n${pantsExportParsed.toJson.prettyPrint}")
}
