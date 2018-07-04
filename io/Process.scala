package pingpong.io

import ammonite.ops._
import com.twitter.util.{Throw, Try, Return, Future}

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
  def invoke(): Future[CommandResult] = Future {
    // Invoke the process in the current directory, wrapping any exception at invocation. This
    // executes the process as well, but we can separate out errors when creating the process vs
    // failed execution by checking the exit code.
    Try(Command(argv, env.overriddenVars, Shellout.executeStream)()(wd = explicitCwd.path))
      .rescue { case e => Throw(ProcessInvocationError("Failed to invoke process.", e))}
      // Make a new exception if the exit code is nonzero.
      .flatMap { result => result.exitCode match {
        case 0 => Return(result)
        case rc => Throw(
          ProcessErrorDuringExecution(s"Execution of request ${this} failed with code ${rc}."))
      }}
      // Return the value from this `Try`, or raise.
      .get()
  }
}

object ExecuteProcessRequest {
  implicit val emptyEnv: ProcessEnvironment = ProcessEnvironment(Map.empty)

  // `import ammonite.ops.ImplicitWd._` can provide the working directory implicit.
  def create(argv: Vector[String], pwd: Directory)(
    implicit env: ProcessEnvironment
  ): ExecuteProcessRequest = ExecuteProcessRequest(argv, env, pwd)
}
