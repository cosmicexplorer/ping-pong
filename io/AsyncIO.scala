package pingpong.io

// FIXME: all the operations in this file are synchronous, behind a Future facade -- it's not clear
// there's a good async file i/o library for the JVM (how???). We may need to create it.
import com.twitter.util.{Throw, Future}

import java.io.{File => JFile}

case class AsyncIOError(message: String) extends Exception(message)

// This is an existing directory.
case class Directory(path: String)

object Directory {
  def fromExistingPath(path: String): Future[Directory] = {
    val filePath = new JFile(path)
    if (filePath.isDirectory()) {
      Future(Directory(path))
    } else {
      Future.const(Throw(AsyncIOError(s"path ${path} does not exist or is not a directory")))
    }
  }
}

case class File(path: String)
