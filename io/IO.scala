package pingpong.io

import ammonite.ops._
// FIXME: all the operations in this file are synchronous, behind a Future facade -- it's not clear
// there's a good async file i/o library for the JVM (how???). We may need to create it.
import com.twitter.util.Future

case class IOError(message: String) extends RuntimeException(message)

sealed trait IOEntity {
  def asStringPath: String
}

// This is an existing directory.
case class Directory(path: Path) extends IOEntity {
  override def asStringPath: String = path.toString
}

object Directory {
  // `Future` apply() runs the body in a separate thread.
  def apply(path: Path): Future[Directory] = {
    maybeExistingDir(path)
      .map {
        case None => throw IOError(s"Path ${path} does not exist or is not a directory.")
        case Some(x) => x
      }
  }

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
}
