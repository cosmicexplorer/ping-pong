package pingpong.ensime

import org.ensime.api._
import org.ensime.sexp._
import org.ensime.sexp.formats._

import org.ensime.sexp._
import shapeless._

import java.nio.file.Paths

case class ExpandedEnsimeConfig(
  // Deprecated.
  rootDir: RawFile,
  cacheDir: RawFile,
  javaHome: RawFile,
  name: String,
  scalaVersion: String,
  // Deprecated.
  compilerArgs: Seq[String],
  javaSources: Seq[RawFile],
  projects: Seq[EnsimeProject],
  // The stuff in ensime-startup.el that isn't in `EnsimeConfig` for some reason.
  ensimeServerJars: Seq[RawFile],
  ensimeServerVersion: String,
  scalaCompilerJars: Seq[RawFile])

// Ripped from `org.ensime.config.EnsimeConfigProtocol`, because all their implicits are private for
// some reason.
object ExpandedEnsimeSexpProtocol {
  object Protocol
      extends DefaultSexpProtocol
      with OptionAltFormat
      with CamelCaseToDashes

  import pingpong.ensime.ExpandedEnsimeSexpProtocol.Protocol._

  implicit object EnsimeFileFormat extends SexpFormat[RawFile] {
    def write(f: RawFile): Sexp = SexpString(f.file.toString)
    def read(sexp: Sexp): RawFile = sexp match {
      case SexpString(file) => RawFile(Paths.get(file))
      case got              => deserializationError(got)
    }
  }

  implicit val projectIdFormat: SexpFormat[EnsimeProjectId] = cachedImplicit
  implicit val projectFormat: SexpFormat[EnsimeProject] = cachedImplicit
  implicit val configFormat: SexpFormat[EnsimeConfig]   = cachedImplicit
  implicit val expandedConfigFormat: SexpFormat[ExpandedEnsimeConfig] = cachedImplicit
}
