package pingpong.io

import ammonite.ops._
// FIXME: all the operations in this file are synchronous, behind a Future facade -- it's not clear
// there's a good async file i/o library for the JVM (how???). We may need to create it.
import com.twitter.util.Future

import java.io.{File => JFile}

sealed trait IOEntity {
  def asStringPath: String
  def asFile: JFile
}

object PathExt {
  implicit class WrappedPath(path: Path) extends IOEntity {
    override def asStringPath: String = path.toString
    override def asFile: JFile = path.toIO
  }

  implicit class WrappedRelPath(path: RelPath) extends IOEntity {
    override def asStringPath: String = path.toString
    // This may have some meaning at some point, but not now.
    // TODO: should we split out the `IOEntity` trait then?
    override def asFile: JFile = ???
  }
}

case class IOError(message: String) extends RuntimeException(message)

// This is an existing directory.
case class Directory(path: Path) extends IOEntity {
  override def asStringPath: String = path.toString
  override def asFile: JFile = path.toIO
}

object Directory {
  def apply(path: Path): Future[Directory] = maybeExistingDir(path).map {
    case None => throw IOError(s"Path ${path} does not exist or is not a directory.")
    case Some(x) => x
  }

  // `Future` apply() runs the body in a separate thread.
  def maybeExistingDir(path: Path): Future[Option[Directory]] = Future {
    if (path.toIO.isDirectory) {
      Some(new Directory(path))
    } else {
      None
    }
  }
}

case class File(path: Path) extends IOEntity {
  override def asStringPath: String = path.toString
  override def asFile: JFile = path.toIO
}
