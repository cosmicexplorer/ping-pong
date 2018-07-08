package pingpong.subsystems

import pingpong.io._
import pingpong.io.PathExt._
import pingpong.io.ProcessExt._
import pingpong.parsing.Regex._
import pingpong.protocol.entities._
import pingpong.protocol.pingpong._
import pingpong.protocol.repo_backend._
import pingpong.protocol.review_backend._

import ammonite.ops._
import com.twitter.util.{Try, Return, Throw, Future}

import scala.sys.process._

class GitError(message: String, base: Throwable) extends RuntimeException(message, base) {
  def this(message: String) = this(message, null)
}

case class GitObjectHashParseError(message: String) extends GitError(message)

case class GitObjectHash(checksum: String)

object GitObjectHash {
  val maxLengthShaPattern = rx"\A[0-9a-f]{40}\Z"

  def apply(checksum: String): Try[GitObjectHash] = maxLengthShaPattern.findFirstIn(checksum)
    .map(validChecksum => Return(new GitObjectHash(validChecksum)))
    .getOrElse(Throw(GitObjectHashParseError(
      s"invalid checksum ${checksum}: string must match ${maxLengthShaPattern}")))
}

case class GitThriftParseError(message: String) extends GitError(message)

trait GitCommandArgument {
  def asCommandArg: String
}

case class GitRevision(objHash: GitObjectHash) extends GitCommandArgument {
  def asThrift = Revision(Some(objHash.checksum))

  override def asCommandArg: String = objHash.checksum
}

object GitRevision {
  def apply(revision: Revision): Try[GitRevision] = revision.backendRevisionSpec
    .map(GitObjectHash(_).map(GitRevision(_)))
    .getOrElse(Throw(GitThriftParseError(
      s"invalid revision ${revision}: revision spec must be provided")))
}

case class GitCloneResultLocalDirectory(source: GitRemote, dir: Directory)

case class GitCacheError(message: String) extends GitError(message)

case class GitCloneBase(dir: Directory)

sealed trait GitRemote extends GitCommandArgument {
  protected def gitRemoteAddress: String

  override def asCommandArg: String = gitRemoteAddress

  // The name to use for the local directory when cloning.
  def localDirname: RelPath

  def asThrift = RepoLocation(Some(gitRemoteAddress))

  def hashDirname: RelPath

  private def getOrCreateCloneDir(baseDir: Directory): Future[Path] = Future {
    val cloneDir = baseDir.path / hashDirname
    mkdir! cloneDir
    cloneDir
  }

  private def fetchVerifyCloneDir(cloneDir: Directory): Future[GitCloneResultLocalDirectory] = {
    val cloneIntoDir = cloneDir.path / localDirname
    // If the clone directory doesn't exist, clone it.
    val clonedDir = Directory.maybeExistingDir(cloneIntoDir).flatMap(_.map(Future(_)).getOrElse {
      Process(Seq("git", "clone", asCommandArg, cloneIntoDir.asStringPath))
        .executeForOutput
        .flatMap(_ => Directory(cloneIntoDir))
    })

    // Verify that the checked-out repo's origin is what we expected.
    clonedDir.flatMap { checkoutDir =>
      Process(Seq("git", "remote", "get-url", "origin"), cwd = checkoutDir.asFile)
        .executeForOutput
        .flatMap { case OutputStrings(stdout, _) => {
          val repoRemote = stdout.trim
          val expectedRemote = gitRemoteAddress
          if (repoRemote == expectedRemote) {
            Future(GitCloneResultLocalDirectory(this, checkoutDir))
          } else {
            val errMsg = (
              s"Clone directory ${checkoutDir} did not point to " +
                s"expected git remote ${expectedRemote} (was: ${repoRemote}).")
            Future.const(Throw(GitCacheError(errMsg)))
          }
        }
        }
    }
  }

  def performClone(cloneBase: GitCloneBase): Future[GitCloneResultLocalDirectory] = {
    getOrCreateCloneDir(cloneBase.dir).flatMap(Directory(_))
      .flatMap(fetchVerifyCloneDir)
  }
}

case class GitInputParseError(message: String) extends GitError(message)

case class LocalFilesystemRepo(rootDir: Path) extends GitRemote {
  override protected def gitRemoteAddress: String = rootDir.toString
  override def localDirname: RelPath = rootDir.last
  override def hashDirname: RelPath = {
    // TODO: generic string join method somewhere!
    val dirJoined = rootDir.segments.reduce((acc, cur) => s"${acc}-${cur}")
    s"local:${dirJoined}"
  }
}

object GitRemote {
  // NB: we would do any parsing of `backendLocationSpec` (e.g. into a url, file path, etc) here
  // and return a different implementor of `GitRemote` for different formats of inputs.
  // Currently, we interpret every string as pointing to a local directory path.
  def apply(location: RepoLocation): Try[GitRemote] = location.backendLocationSpec match {
    case Some(locationSpec) => GitRemote(locationSpec)
    case None => Throw(GitInputParseError(
      s"location ${location} was not provided"))
  }

  def apply(locationSpec: String): Try[GitRemote] = Return(LocalFilesystemRepo(Path(locationSpec)))
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

object GitCheckedOutWorktree {
  def apply(checkout: Checkout): Future[GitCheckedOutWorktree] = ???
}

case class GitCheckoutRequest(source: GitRemote, revision: GitRevision) {
  def asThrift = CheckoutRequest(Some(source.asThrift), Some(revision.asThrift))

  def hashDirname: RelPath = s"${source.hashDirname}@${revision.asCommandArg}"

  private def getOrCreateWorktreeDir(baseDir: Directory): Future[Path] = Future {
    val worktreeDir = baseDir.path / hashDirname
    mkdir! worktreeDir
    worktreeDir
  }

  private def createVerifyWorktreeDir(
    cloneResult: GitCloneResultLocalDirectory, worktreeDir: Directory
  ): Future[GitCheckedOutWorktree] = {
    val intoWorktreeDir = worktreeDir.path / cloneResult.source.localDirname
    // If the worktree directory doesn't exist, clone it.
    val worktreeDirCloned = Directory.maybeExistingDir(intoWorktreeDir)
      .flatMap(_.map(Future(_)).getOrElse {
        val processRequest = Process(
          Seq("git", "worktree", "add", intoWorktreeDir.asStringPath, revision.asCommandArg),
          cwd = cloneResult.dir.asFile)

        processRequest.executeForOutput
          .flatMap(_ => Directory(intoWorktreeDir))
      })

    // Verify that the checked-out worktree is at the right revision.
    worktreeDirCloned.flatMap { checkedOutWorktreeDir =>
      Process(Seq("git", "rev-parse", "HEAD"), cwd = checkedOutWorktreeDir.asFile)
        .executeForOutput
        .flatMap { case OutputStrings(stdout, _) => {
          val repoRevision = stdout.trim
          val expectedRevision = revision.asCommandArg
          if (repoRevision == expectedRevision) {
            Future(GitCheckedOutWorktree(cloneResult, revision, checkedOutWorktreeDir))
          } else {
            val errMsg = (
              s"Worktree directory ${checkedOutWorktreeDir} did not point to " +
                s"expected revision ${revision} (was: ${repoRevision}).")
            Future.const(Throw(GitCacheError(errMsg)))
          }
        }}
    }
  }

  private def createWorktreeForRevision(
    cloneResult: GitCloneResultLocalDirectory, worktreeBase: GitWorktreeBase,
  ): Future[GitCheckedOutWorktree] = {
    getOrCreateWorktreeDir(worktreeBase.dir).flatMap(Directory(_))
      .flatMap(createVerifyWorktreeDir(cloneResult, _))
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

case class GitIdentificationError(message: String) extends GitError(message)

// TODO: what other notions of identity should we consider? This is probably fine for a first draft.
case class GitUser(email: String) {
  def asThrift = UserId(Some(email))
}

object GitUser {
  // From https://stackoverflow.com/a/13087910/2518889 -- should use a more legitimate solution, but
  // email as the sole identifier should be revised anyway.
  val emailRegex = rx"\A([0-9a-zA-Z](?>[-.\w]*[0-9a-zA-Z])*@(?>[0-9a-zA-Z][-\w]*[0-9a-zA-Z]\.)+[a-zA-Z]{2,9})\Z"

  def apply(gitId: UserId): Try[GitUser] = Try {
    gitId.uid
      .map(GitUser(_).get)
      .head
  }

  def apply(email: String): Try[GitUser] = {
    emailRegex.findFirstIn(email) match {
      case Some(validEmail) => Return(new GitUser(validEmail))
      case None => Throw(GitIdentificationError(
        s"invalid email ${email}: email address must be provided and match ${emailRegex}"))
    }
  }
}

case class GitRevisionRange(begin: GitRevision, end: GitRevision) extends GitCommandArgument {
  override def asCommandArg: String = s"${begin.asCommandArg}..${end.asCommandArg}"

  def asThrift = RevisionRange(Some(asCommandArg))
}

object GitRevisionRange {
  val maxLengthShaRange = rx"\A([0-9a-f]{40})\.\.([0-9a-f]{40})\Z"

  def apply(range: RevisionRange): Try[GitRevisionRange] = range.backendRevisionRangeSpec
    .map(GitRevisionRange(_))
    .getOrElse(Throw(GitInputParseError(
      s"invalid revision range ${range}: range must be provided")))

  def apply(rangeSpec: String): Try[GitRevisionRange] = rangeSpec match {
    case maxLengthShaRange(begin, end) => {
      val begRev = GitRevision(Revision(Some(begin)))
      val endRev = GitRevision(Revision(Some(end)))
      begRev.flatMap(b => endRev.map(GitRevisionRange(b, _)))
    }
    case _ => Throw(GitInputParseError(
      s"invalid revision range ${rangeSpec}: range must match ${maxLengthShaRange}"))
  }
}

case class GitRepoCommunicationError(message: String, base: Throwable)
    extends GitError(message, base)

case class GitCollaborationResolutionError(message: String) extends GitError(message)

case class GitNotesCollaboration()

case class GitNotesCollaborationId(source: GitRemote, revisionRange: GitRevisionRange) {
  private def asCollabIdString = s"${source.asCommandArg}:${revisionRange.asCommandArg}"

  def asThrift = CollaborationId(Some(asCollabIdString))

  private def pingsForCollaboration(checkout: GitCheckedOutWorktree): Future[PingCollection] = ???

  def getCollaboration(gitRepo: RepoBackend.MethodPerEndpoint): Future[Collaboration] = {
    val checkoutRequest = GitCheckoutRequest(source, revisionRange.end)

    gitRepo.getCheckout(checkoutRequest.asThrift).flatMap {
      case CheckoutResponse.Error(err) => Future.const(Throw(
        GitRepoCommunicationError(s"error checking out collaboration request ${this}", err)))
      case x: CheckoutResponse.UnknownUnionField => Future.const(Throw(
        GitCollaborationResolutionError(
          s"error checking out collaboration request ${this}: " +
            s"unknown union for checkout response ${x}")))
      case CheckoutResponse.Completed(checkout) => GitCheckedOutWorktree(checkout)
          .map(pingsForCollaboration(_))
          .map(pings => Collaboration(Some(checkout), Some(pings)))
    }
  }
}

object GitNotesCollaborationId {
  val collabRequestPattern = rx"\A([^:]+):([^:]+)\Z"

  def apply(collabId: CollaborationId): Try[GitNotesCollaborationId] = collabId.cid match {
    case Some(collabRequestPattern(remoteSpec, rangeSpec)) => {
      val remote = GitRemote(remoteSpec)
      val range = GitRevisionRange(rangeSpec)
      remote.flatMap(src => range.map(GitNotesCollaborationId(src, _)))
    }
    case _ => Throw(GitInputParseError(
      s"invalid collaboration id ${collabId}: id must be provided and match ${collabRequestPattern}"
    ))
  }
}

// case class GitNotesCollaborationQuery()

case class GitNotesPing(gitObjectHash: GitObjectHash, ping: Ping)

object GitNotesPing {
  def apply(ping: Ping): Future[GitNotesPing] = {
    // FIXME: **NEED** a cwd!!!
    Process(Seq("git", "hash-object", "-w", "--stdin"))
      .executeForOutput(ping.toBinaryStdin)
      .flatMap { case OutputStrings(stdout, _) => Future.const(GitObjectHash(stdout.trim)) }
      .map(GitNotesPing(_, ping))
  }
}
