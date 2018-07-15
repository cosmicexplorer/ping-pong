package pingpong.util

import java.io.{StringWriter, PrintWriter}

object ErrorExt {
  implicit class ThrowableWrapper(e: Throwable) {
    def asStackTrace: String = {
      val sw = new StringWriter
      e.printStackTrace(new PrintWriter(sw))
      sw.toString
    }
  }
}
