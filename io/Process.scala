package pingpong.io

import com.twitter.scrooge.ThriftStruct
import com.twitter.util.{Throw, Future}
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TIOStreamTransport

import scala.sys.process.{BasicIO, ProcessLogger, ProcessBuilder}

import java.io.OutputStream

class ProcessError(message: String, base: Throwable) extends RuntimeException(message, base) {
  def this(message: String) = this(message, null)
}

case class ProcessErrorDuringExecution(message: String) extends ProcessError(message)

case class ProcessEnvironment(overriddenVars: Map[String, String]) {
  // Use this with `: _*` to more hygienically pass environment vars to a `ProcessBuilder`.
  def toArgs = overriddenVars.toSeq
}

object ProcessExt {
  type ProcessInputProcessor = OutputStream => Unit

  implicit val closedStdin: ProcessInputProcessor = _.close()

  case class OutputStrings(stdout: String, stderr: String)

  implicit class WrappedProcess(scalaProc: ProcessBuilder) {
    // NB: We don't have a separate method to execute closing all output streams so that we can rely
    // on having the output in the error message if the exit code is nonzero.
    def executeForOutput(implicit inputFun: ProcessInputProcessor): Future[OutputStrings] = {
      // NB: `StringBuilder` is not synchronized! This is fine because we only modify it within the
      // `Future {}` nullary method constructor below.
      val stdoutBuilder = new StringBuilder()
      val stderrBuilder = new StringBuilder()
      // TODO: I'm assuming `ProcessLogger` closes the output streams when done? Should make sure of
      // this.
      val procLogger = ProcessLogger(fout = stdoutBuilder.append(_), ferr = stderrBuilder.append(_))
      // `withIn = false` means we do not hook up the subprocess's stdin to the parent process's
      // stdin.
      val procIO = BasicIO(withIn = false, log = procLogger).withInput(inputFun)
      val exitStatus = Future { scalaProc.run(procIO).exitValue }
      exitStatus.flatMap { rc =>
        val allOutput = OutputStrings(
          stdout = stdoutBuilder.toString,
          stderr = stderrBuilder.toString)
        rc match {
          case 0 => Future(allOutput)
          case _ => Future.const(Throw(ProcessErrorDuringExecution(
            s"Execution of process ${this} failed with code ${rc}.\n\n" +
              s"stdout:\n${allOutput.stdout}\n\n" +
              s"stderr:\n${allOutput.stderr}")
          ))
        }
      }
    }
  }

  implicit class WrappedThriftStruct[T <: ThriftStruct](thriftObj: T) {
    def toBinaryStdin: ProcessInputProcessor = { processStdin =>
      val thriftStreamInput = new TIOStreamTransport(processStdin)
      val binaryProtocol = new TBinaryProtocol(thriftStreamInput)
      thriftObj.write(binaryProtocol)
      processStdin.close()
    }
  }
}
