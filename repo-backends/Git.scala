package pingpong.repo_backends

import pingpong.io._
import pingpong.io.ExecuteProcessRequest._
import pingpong.parsing.Regex._
import pingpong.protocol.repo_backend._

import ammonite.ops._
import com.twitter.util.{Return, Throw, Try, Future}

class GitRepoError(message: String, base: Throwable) extends RuntimeException(message, base) {
  def this(message: String) = this(message, null)
}

case class GitInputParseError(message: String) extends GitRepoError(message)

case class GitProcessInvocationError(message: String, base: Throwable)
    extends GitRepoError(message, base)

case class GitRevision(sha: String) {
  def asThrift = Revision(Some(sha))
}

object GitRevision {
  val maxLengthShaPattern = rx"\A[0-9a-f]{40}\Z"
  def apply(revision: Revision): Try[GitRevision] = {
    revision.backendRevisionSpec
      .flatMap(maxLengthShaPattern.findFirstIn(_)) match {
        case Some(validRevisionSpec) => Return(GitRevision(validRevisionSpec))
        case None => Throw(GitInputParseError(
          s"invalid revision ${revision}: string must exist and match ${maxLengthShaPattern}"))
      }
  }
}

sealed trait GitRemote {
  protected def gitRemoteAddress: String
  // The name to use for the local directory when cloning.
  protected def localDirname: RelPath

  def asThrift = RepoLocation(Some(gitRemoteAddress))

  private def getTmpDir(): Future[Path] = Future {
    tmp.dir(deleteOnExit = false)
  }

  private def asProcessExecution(): Future[(ExecuteProcessRequest, Path)] = {
    getTmpDir().flatMap(Directory(_)).map(_.path / localDirname).flatMap { cloneDirPath =>
      val gitCloneRequest = Directory(pwd).map { wd => ExecuteProcessRequest.create(
        Vector("git", "clone", gitRemoteAddress, cloneDirPath.toString),
        pwd = wd)
      }
      gitCloneRequest.map((_, cloneDirPath))
    }
  }

  def performClone(): Future[Directory] = asProcessExecution()
    .flatMap { case (processRequest, resultingPath) => processRequest.invoke()
      .flatMap(_ => Directory(resultingPath))
    }
}

case class LocalFilesystemRepo(rootDir: Path) extends GitRemote {
  override protected def gitRemoteAddress: String = rootDir.toString
  override protected def localDirname: RelPath = RelPath(rootDir.last)
}

object GitRemote {
  def apply(location: RepoLocation): Try[GitRemote] = {
    // NB: we would do any parsing of `backendLocationSpec` (e.g. into a url, file path, etc) here
    // and return a different implementor of `GitRemote` for different formats of inputs.
    // Currently, we interpret every string as pointing to a local directory path.
    location.backendLocationSpec
      .map(spec => Return(LocalFilesystemRepo(Path(spec))))
      .head
  }
}

case class GitCheckoutRequest(source: GitRemote, revision: GitRevision) {
  private def checkoutProcessRequest(clonedDir: Directory) = ExecuteProcessRequest.create(
    Vector("git", "checkout", revision.sha),
    pwd = clonedDir)

  // TODO: caching, cleanup, etc
  def checkout(): Future[GitCheckout] = source.performClone().flatMap { clonedDir =>
    checkoutProcessRequest(clonedDir).invoke()
      .map(_ => GitCheckout(clonedDir, source, revision))
  }
}

object GitCheckoutRequest {
  def apply(request: CheckoutRequest): Try[GitCheckoutRequest] = {
    val source = request.source
      .map(GitRemote(_))
      .head
    val revision = request.revision
      .map(GitRevision(_))
      .head

    source.flatMap(src => revision.map(rev => GitCheckoutRequest(src, rev)))
  }
}

// Here, `sandboxRoot` is where the repo is checked out on disk, and `source` is where to clone it
// from.
case class GitCheckout(sandboxRoot: Directory, source: GitRemote, revision: GitRevision) {
  def asThrift = {
    val checkoutLocation = CheckoutLocation(Some(sandboxRoot.asStringPath))
    Checkout(Some(checkoutLocation), Some(source.asThrift), Some(revision.asThrift))
  }
}

class GitRepoBackend extends RepoBackend.MethodPerEndpoint {
  override def getCheckout(request: CheckoutRequest): Future[CheckoutResponse] = {
    val wrappedRequest = GitCheckoutRequest(request)
    val checkoutExecution = Future.const(wrappedRequest).flatMap(_.checkout())

    checkoutExecution
      .map(checkout => CheckoutResponse.Completed(checkout.asThrift))
      .rescue { case e => Future(CheckoutResponse.Error(RepoBackendError(Some(e.toString)))) }
  }
}
