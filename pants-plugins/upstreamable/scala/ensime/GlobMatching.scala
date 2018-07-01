package pingpong.ensime

import java.nio.file.{FileSystems, Path, SimpleFileVisitor}

class GlobMatcher(glob: String) extends SimpleFileVisitor[Path] {
  val matcher = FileSystems.getDefault.getPathMatcher(s"glob:${glob}")

  def expand(fromDir: Path): Seq[Path] = ???
}
