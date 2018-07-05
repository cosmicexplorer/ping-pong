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

case class GitCacheError(message: String) extends GitRepoError(message)

case class GitProcessInvocationError(message: String, base: Throwable)
    extends GitRepoError(message, base)

case class GitRevision(sha: String) {
  def asThrift = Revision(Some(sha))
}

object GitRevision {
  val maxLengthShaPattern = rx"\A[0-9a-f]{40}\Z"

  def apply(revision: Revision): Try[GitRevision] = Try {
    revision.backendRevisionSpec
      .map(GitRevision(_).get())
      .head
  }

  def apply(sha: String): Try[GitRevision] = {
    maxLengthShaPattern.findFirstIn(sha) match {
      case Some(validRevisionSpec) => Return(new GitRevision(validRevisionSpec))
      case None => Throw(GitInputParseError(
        s"invalid revision ${sha}: string must exist and match ${maxLengthShaPattern}"))
    }
  }
}

case class GitCloneBase(dir: Directory)

case class GitCloneResultLocalDirectory(source: GitRemote, dir: Directory)

sealed trait GitRemote {
  protected def gitRemoteAddress: String
  // The name to use for the local directory when cloning.
  def localDirname: RelPath

  def asThrift = RepoLocation(Some(gitRemoteAddress))

  def hashDirname: RelPath

  private def getOrCreateCloneDir(baseDir: Directory): Future[Path] = Future {
    val cloneDir = baseDir.path / hashDirname
    mkdir! cloneDir
    cloneDir
  }

  private def asProcessExecution(cloneIntoDir: Path): Future[ExecuteProcessRequest] = {
    Directory(pwd).map { wd =>
      ExecuteProcessRequest.create(
        Vector("git", "clone", gitRemoteAddress, cloneIntoDir.toString),
        pwd = wd)
    }
  }

  private def verifyCloneDirOrFetch(cloneDir: Directory): Future[GitCloneResultLocalDirectory] = {
    val cloneIntoDir = cloneDir.path / localDirname
    Directory.maybeExistingDir(cloneIntoDir).flatMap { dirOpt =>
      dirOpt.map { cloneSameRepoDir =>
        val gitRemoteRequest = ExecuteProcessRequest.create(
          Vector("git", "remote", "get-url", "origin"),
          pwd = cloneSameRepoDir)
        gitRemoteRequest.invoke()
          .flatMap { result =>
            val remoteStr = result.out.trim
            val expectedRemote = gitRemoteAddress
            if (remoteStr == expectedRemote) {
              Future(GitCloneResultLocalDirectory(this, cloneSameRepoDir))
            } else {
              val errMsg = (
                s"Clone directory ${cloneSameRepoDir} did not point to " +
                  s"expected git remote ${expectedRemote}.")
              Future.const(Throw(GitCacheError(errMsg)))
            }
          }
      }
        .getOrElse {
          val gitCloneRequest = asProcessExecution(cloneIntoDir)
          gitCloneRequest
            .flatMap { req =>
              req.invoke()
                .rescue { case e => Future.const(Throw(GitCacheError(
                  s"Clone request ${req} failed.")))
                }
                .flatMap(_ => Directory(cloneIntoDir)
                  .map(GitCloneResultLocalDirectory(this, _))
                  .rescue { case e => Future.const(Throw {
                    val errMsg = (
                      s"Clone request ${req} succeeded, but failed to create " +
                        s"directory ${cloneIntoDir}.")
                    GitCacheError(errMsg)
                  })})
            }
        }
    }
  }

  def performClone(cloneBase: GitCloneBase): Future[GitCloneResultLocalDirectory] = {
    getOrCreateCloneDir(cloneBase.dir).flatMap(Directory(_))
      .flatMap(verifyCloneDirOrFetch)
  }
}

case class LocalFilesystemRepo(rootDir: Path) extends GitRemote {
  override protected def gitRemoteAddress: String = rootDir.toString
  override def localDirname: RelPath = RelPath(rootDir.last)
  override def hashDirname: RelPath = {
    val dirJoined = rootDir.segments.reduce((acc, cur) => s"${acc}-${cur}")
    s"local:${dirJoined}"
  }
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

case class GitWorktreeBase(dir: Directory)

case class GitRepoParams(cloneBase: GitCloneBase, worktreeBase: GitWorktreeBase)

case class GitCheckedOutWorktree(
  cloneResult: GitCloneResultLocalDirectory,
  revision: GitRevision,
  dir: Directory,
) {
  def asThrift = {
    val checkoutLocation = CheckoutLocation(Some(dir.asStringPath))
    Checkout(Some(checkoutLocation), Some(cloneResult.source.asThrift), Some(revision.asThrift))
  }
}

case class GitCheckoutRequest(source: GitRemote, revision: GitRevision) {
  private def worktreeCheckoutRequest(
    cloneResult: GitCloneResultLocalDirectory, intoWorktreeDir: Path,
  ) = ExecuteProcessRequest.create(
    Vector("git", "worktree", "add", intoWorktreeDir.toString, revision.sha),
    pwd = cloneResult.dir)

  def hashDirname: RelPath = s"${source.hashDirname}@${revision.sha}"

  private def getOrCreateWorktreeDir(baseDir: Directory): Future[Path] = Future {
    val worktreeDir = baseDir.path / hashDirname
    mkdir! worktreeDir
    worktreeDir
  }

  private def verifyWorktreeDirOrCreate(
    cloneResult: GitCloneResultLocalDirectory, worktreeDir: Directory,
  ): Future[GitCheckedOutWorktree] = {
    val intoWorktreeDir = worktreeDir.path / cloneResult.source.localDirname
    Directory.maybeExistingDir(intoWorktreeDir).flatMap { dirOpt =>
      dirOpt.map { worktreeExistsDir =>
        val gitRevisionCheckRequest = ExecuteProcessRequest.create(
          Vector("git", "rev-parse", "HEAD"),
          pwd = worktreeExistsDir)
        gitRevisionCheckRequest.invoke()
          .flatMap { result =>
            Future.const(GitRevision(result.out.trim))
              .flatMap { worktreeRevision =>
                if (worktreeRevision == revision) {
                  Future(GitCheckedOutWorktree(cloneResult, revision, worktreeExistsDir))
                } else {
                  val errMsg = (
                    s"Worktree directory ${worktreeExistsDir} did not point to " +
                      s"expected revision ${revision}.")
                  Future.const(Throw(GitCacheError(errMsg)))
                }
              }
          }
      }
        .getOrElse {
          val gitWorktreeRequest = worktreeCheckoutRequest(cloneResult, intoWorktreeDir)
          gitWorktreeRequest.invoke()
            .rescue { case e => Future.const(Throw(GitCacheError(
              s"Worktree checkout request ${gitWorktreeRequest} failed.")))
            }
            .flatMap(_ => Directory(intoWorktreeDir)
              .map(GitCheckedOutWorktree(cloneResult, revision, _))
              .rescue { case e => Future.const(Throw {
                val errMsg = (
                  s"Worktree checkout request ${gitWorktreeRequest} succeeded, but failed " +
                    s"to create directory ${intoWorktreeDir}.")
                GitCacheError(errMsg)
              })})
        }
    }
  }

  private def createWorktreeForRevision(
    cloneResult: GitCloneResultLocalDirectory, worktreeBase: GitWorktreeBase,
  ): Future[GitCheckedOutWorktree] = {
    getOrCreateWorktreeDir(worktreeBase.dir).flatMap(Directory(_))
      .flatMap(verifyWorktreeDirOrCreate(cloneResult, _))
  }

  def checkout(params: GitRepoParams): Future[GitCheckedOutWorktree] = {
    source.performClone(params.cloneBase)
      .flatMap(createWorktreeForRevision(_, params.worktreeBase))
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

class GitRepoBackend(repoParams: GitRepoParams) extends RepoBackend.MethodPerEndpoint {
  // Use worktrees from a single clone, instead of doing a new clone each time. Keep a base clone of
  // each remote in `repoParams.cloneBase`, then keep each checkout in a worktree branched from that
  // clone, under `repoParams.worktreeBase`, using directory paths created from the hash of the
  // request. If the hash matches, we check that the checkout actually corresponds to the request!
  // TODO: cleanup LRU checkouts after we take up enough space!
  override def getCheckout(request: CheckoutRequest): Future[CheckoutResponse] = {
    val wrappedRequest = GitCheckoutRequest(request)
    val checkoutExecution = Future.const(wrappedRequest).flatMap(_.checkout(repoParams))

    checkoutExecution
      .map(checkout => CheckoutResponse.Completed(checkout.asThrift))
      .rescue { case e => Future(CheckoutResponse.Error(RepoBackendError(Some(e.toString)))) }
  }
}
