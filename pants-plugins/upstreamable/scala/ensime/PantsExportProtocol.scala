package pingpong.ensime

import spray.json._

case class SourceRoot(sourceRootPath: String, packagePrefix: String)

case class Globs(exclude: Option[Seq[Globs]], globs: Seq[String])

case class Target(
  dependencies: Option[Seq[String]],
  targetType: String,
  scope: String,
  roots: Seq[SourceRoot],
  isTargetRoot: Boolean,
  excludes: Option[Seq[String]],
  id: String,
  globs: Globs,
  libraries: Seq[String],
  transitive: Boolean,
  isCodeGen: Boolean,
  platform: Option[String],
  isSynthetic: Boolean)

case class JvmPlatform(sourceLevel: String, args: Seq[String], targetLevel: String)

case class JvmPlatformDict(platforms: Map[String, JvmPlatform], defaultPlatform: String)

case class PreferredJvmDistribution(strict: String, nonStrict: String)

case class Library(defaultClasspath: Option[String])

case class PantsExport(
  targets: Map[String, Target],
  jvmPlatforms: JvmPlatformDict,
  preferredJvmDistributions: Map[String, PreferredJvmDistribution],
  version: String,
  libraries: Map[String, Library])

object PantsExportProtocol extends DefaultJsonProtocol {
  implicit val sourceRootFormat = jsonFormat(SourceRoot, "source_root", "package_prefix")
  implicit val globsFormat: JsonFormat[Globs] = lazyFormat(jsonFormat(Globs, "exclude", "globs"))
  implicit val targetFormat = jsonFormat(
    Target,
    "targets", "pants_target_type", "scope", "roots", "is_target_root", "excludes", "id",
    "globs", "libraries", "transitive", "is_code_gen", "platform", "is_synthetic")
  implicit val jvmPlatformFormat = jsonFormat(JvmPlatform, "source_level", "args", "target_level")
  implicit val jvmPlatformDictFormat = jsonFormat(JvmPlatformDict, "platforms", "default_platform")
  implicit val preferredJvmDistFormat = jsonFormat(PreferredJvmDistribution, "strict", "non_strict")
  implicit val libraryFormat = jsonFormat(Library, "default")
  implicit val pantsExportFormat = jsonFormat(
    PantsExport, "targets", "jvm_platforms", "preferred_jvm_distributions", "version", "libraries")
}
