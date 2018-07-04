package pingpong.io

import ammonite.ops._
import com.twitter.util.{Throw, Future}

class ProcessError(message: String, base: Throwable) extends RuntimeException(message, base) {
  def this(message: String) = this(message, null)
}

case class ProcessInvocationError(message: String, base: Throwable)
    extends ProcessError(message, base)

case class ProcessErrorDuringExecution(message: String) extends ProcessError(message)

case class ProcessEnvironment(overriddenVars: Map[String, String])

case class ExecuteProcessRequest(
  argv: Vector[String],
  env: ProcessEnvironment,
  explicitCwd: Directory,
) {
  // `Future` apply() runs the body in a separate thread.
  private def makeCommand(): Future[CommandResult] = Future {
    Command(argv, env.overriddenVars, Shellout.executeStream)()(wd = explicitCwd.path)
  }

  def invoke(): Future[CommandResult] = {
    // Invoke the process in the current directory, wrapping any exception at invocation. This
    // executes the process as well, but we can separate out errors when creating the process vs
    // failed execution by checking the exit code.
    makeCommand()
      .rescue { case e => Future.const(Throw(
        ProcessInvocationError("Failed to invoke process.", e)))
      }
      .flatMap { result =>
        // Make a new exception if the exit code is nonzero.
        val rc = result.exitCode
        if (rc == 0) {
          Future(result)
        } else {
          Future.const(Throw(
            ProcessErrorDuringExecution(s"Execution of request ${this} failed with code ${rc}.")))
        }
      }
  }
}

object ExecuteProcessRequest {
  implicit val emptyEnv: ProcessEnvironment = ProcessEnvironment(Map.empty)

  // `import ammonite.ops.ImplicitWd._` can provide the working directory implicit.
  def create(argv: Vector[String], pwd: Directory)(
    implicit env: ProcessEnvironment
  ): ExecuteProcessRequest = ExecuteProcessRequest(argv, env, pwd)
}
