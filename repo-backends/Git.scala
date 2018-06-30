package pingpong.repo_backends

import pingpong.protocol.repo_backend.{Revision, PathGlob, GetSandboxGlobsRequest, GetSandboxGlobsResponse, RepoBackend, RepoLocation, Sandbox, RepoFile, SandboxWithExpandedGlobs, RepoBackendError}

import com.twitter.util.{Return, Throw, Try, Future}

import java.io.File

case class GitRepoError(message: String) extends Exception(message)

case class GitGlob(repoRootRelativeGlob: String)

object GitGlob {
  // TODO: make this use the r"..." form (create implicit if necessary)!
  val validGlobPattern = raw"\A[0-9a-zA-Z/_\-]+\Z".r
  def apply(glob: PathGlob): Try[GitGlob] = {
    glob.relativeGlob
      .flatMap(validGlobPattern.findFirstIn(_))
      .map(validGlob => Return(GitGlob(validGlob)))
      .getOrElse {
        Throw(GitRepoError(
          s"invalid glob ${glob}: string must exist and match ${validGlobPattern}"))
      }
  }
}

case class GitRevision(sha: String)

object GitRevision {
  val maxLengthShaPattern = raw"\A[0-9a-f]{40}\Z".r
  def apply(revision: Revision): Try[GitRevision] = {
    revision.backendRevisionSpec
      .flatMap(maxLengthShaPattern.findFirstIn(_))
      .map(validRevisionSpec => Return(GitRevision(validRevisionSpec)))
      .getOrElse {
        Throw(GitRepoError(
          s"invalid revision ${revision}: string must exist and match ${maxLengthShaPattern}"))
      }
  }
}


sealed trait RepoRoot

case class LocalRepoRoot(rootDir: File) extends RepoRoot

object LocalRepoRoot {
  def apply(dirPath: String): Try[LocalRepoRoot] = {
    val path = new File(dirPath)
    if (path.isAbsolute() && path.isDirectory()) {
      Return(LocalRepoRoot(path))
    } else {
      Throw(GitRepoError(
        s"invalid local repo root ${path}: must be an absolute path to an existing directory"))
    }
  }
}

object RepoRoot {
  def apply(location: RepoLocation): Try[RepoRoot] = {
    location.backendLocationSpec
      .map(LocalRepoRoot(_))
      .head
  }
}

case class GitGlobsRequest(revision: GitRevision, globs: Seq[GitGlob], location: RepoRoot)

object GitGlobsRequest {
  def apply(request: GetSandboxGlobsRequest): Try[GitGlobsRequest] = {
    val revision = request.revision
      .map(GitRevision(_))
      .head
    val globs = request.pathGlobs
      .filter(!_.isEmpty)
      .map(pg => Try(pg.map(GitGlob(_).get)))
      .head
    val repoLocation = request.repoLocation
      .map(RepoRoot(_))
      .head

    // TODO: make it easier to join `Try`s (or `Future`s) -- see /util/TryUtils.scala!
    revision.andThen(rev => globs.andThen(pg => repoLocation.map((rev, pg, _))))
      .map { case (rev, pg, loc) => GitGlobsRequest(rev, pg, loc) }
  }
}

case class GitSandbox(sandboxRoot: File) {
  def asThrift = Sandbox(Some(sandboxRoot.getPath()))
}

case class SandboxFile(relativePath: File) {
  def asThrift = RepoFile(Some(relativePath.getPath()))
}

case class SandboxWithExpansions(sandbox: GitSandbox, expandedGlobs: Seq[SandboxFile]) {
  def asThrift = SandboxWithExpandedGlobs(
    Some(sandbox.asThrift),
    Some(expandedGlobs.map(_.asThrift)))
}

class GitRepoBackend extends RepoBackend.MethodPerEndpoint {
  override def getSandboxGlobs(request: GetSandboxGlobsRequest): Future[GetSandboxGlobsResponse] = {
    val unwrappedRequest = GitGlobsRequest(request)
    Future(GetSandboxGlobsResponse.Error(RepoBackendError(Some("huh"))))
  }
}
