package pingpong.subsystems

import pingpong.io._
import pingpong.io.PathExt._
import pingpong.io.ProcessExt._
import pingpong.parsing._
import pingpong.parsing.Regex._
import pingpong.protocol.entities._
import pingpong.protocol.notifications._
import pingpong.protocol.pingpong._
import pingpong.protocol.repo_backend._
import pingpong.protocol.review_backend._
import pingpong.util.FutureTryExt._
import pingpong.util.StringExt._

import ammonite.ops._
import com.twitter.util.{Try, Return, Throw, Future}
import com.twitter.scrooge.ThriftStruct

import scala.sys.process._

class GitError(message: String, base: Throwable) extends RuntimeException(message, base) {
  def this(message: String) = this(message, null)
}

case class GitObjectHashParseError(message: String) extends GitError(message)

// FIXME: this is only useful if we force its use for process creation! And when we do that, it
// should probably be done in `pingpong.io`.
trait GitCommandArg extends HasCanonicalString {
  def asCommandLineArg: String = asCanonicalString
}

trait GitThriftParser[T <: ThriftStruct, S <: Thriftable[T]] extends ThriftParser[T, S] {
  implicit def thriftParseOptionalDerefErrorFactory[O](fieldName: String): Try[O] =
    Throw(GitThriftParseError(s"field ${fieldName} must be provided"))

  override def thriftParseErrorWrapper(description: String, arg: T, theTry: Try[S]): Try[S] =
    theTry.rescue {
      case e: GitError => Throw(GitObjectMarshalError(s"${description} ${arg} is invalid", e))
    }
}

trait GitStringParser[T <: HasCanonicalString] extends StringParser[T] {
  override def stringParseErrorWrapper(description: String, arg: String, theTry: Try[T]): Try[T] =
    theTry.rescue {
      case e: GitError => Throw(GitStringParseError(s"${description} '${arg}' is invalid", e))
    }
}

case class GitObjectHash(checksum: String) extends GitCommandArg {
  override def asCanonicalString: String = checksum
}

case class GitStringParseError(message: String, base: Throwable) extends GitError(message, base)

object GitObjectHash extends GitStringParser[GitObjectHash] {
  val maxLengthShaPattern = rx"\A[0-9a-f]{40}\Z"

  def apply(checksum: String) = asStringParse("checksum", checksum,
    maxLengthShaPattern.findFirstIn(checksum) match {
      case Some(validChecksum) => Return(new GitObjectHash(validChecksum))
      case None => Throw(GitObjectHashParseError(s"string must match ${maxLengthShaPattern}"))
    })
}

case class GitThriftParseError(message: String) extends GitError(message)

case class GitObjectMarshalError(message: String, base: GitError) extends GitError(message, base)

case class GitRevision(objHash: GitObjectHash) extends Thriftable[Revision] with GitCommandArg {
  override val asThrift = Revision(Some(objHash.checksum))
  override def asCanonicalString: String = objHash.asCanonicalString
}

object GitRevision
    extends GitThriftParser[Revision, GitRevision]
    with GitStringParser[GitRevision] {
  override def apply(revision: Revision) = asThriftParse("revision", revision,
    revision.backendRevisionSpec.derefOptionalField("revision specification")
      .flatMap(GitRevision(_).asTry))

  override def apply(checksum: String) = asStringParse("checksum", checksum,
    GitObjectHash(checksum).asTry.map(new GitRevision(_)))
}

case class GitCloneResultLocalDirectory(source: GitRemote, dir: Directory)

case class GitCacheError(message: String) extends GitError(message)

case class GitCloneBase(dir: Directory)

sealed trait GitRemote extends GitCommandArg with Thriftable[RepoLocation] {
  protected def gitRemoteAddress: String

  override def asCanonicalString: String = gitRemoteAddress

  // The name to use for the local directory when cloning.
  def localDirname: RelPath

  override def asThrift = RepoLocation(Some(gitRemoteAddress))

  def hashDirname: RelPath

  private def getOrCreateCloneDir(baseDir: Directory): Future[Path] = Future {
    val cloneDir = baseDir.path / hashDirname
    mkdir! cloneDir
    cloneDir
  }

  private def fetchVerifyCloneDir(cloneDir: Directory): Future[GitCloneResultLocalDirectory] = {
    val cloneIntoDir = cloneDir.path / localDirname
    // If the clone directory doesn't exist, clone it.
    val clonedDir = Directory.maybeExistingDir(cloneIntoDir).flatMap {
      case Some(existingClone) => Future(existingClone)
      case None => Process(Seq("git", "clone", asCommandLineArg, cloneIntoDir.asStringPath))
          .executeForOutput
          .flatMap(_ => Directory(cloneIntoDir))
    }

    // Verify that the checked-out repo's origin is what we expected.
    clonedDir.flatMap { checkoutDir =>
      Process(Seq("git", "remote", "get-url", "origin"), cwd = checkoutDir.asFile)
        .executeForOutput
        .flatTry { case OutputStrings(stdout, _) => {
          val repoRemote = stdout.trim
          val expectedRemote = gitRemoteAddress
          if (repoRemote == expectedRemote) {
            Return(GitCloneResultLocalDirectory(this, checkoutDir))
          } else {
            val errMsg = (
              s"Clone directory ${checkoutDir} did not point to " +
                s"expected git remote ${expectedRemote} (was: ${repoRemote}).")
            Throw(GitCacheError(errMsg))
          }
        }
        }
    }
  }

  def performClone(cloneBase: GitCloneBase): Future[GitCloneResultLocalDirectory] =
    getOrCreateCloneDir(cloneBase.dir)
      .flatMap(Directory(_))
      .flatMap(fetchVerifyCloneDir)
}

case class GitInputParseError(message: String) extends GitError(message)

case class LocalFilesystemRepo(rootDir: Path) extends GitRemote {
  override protected def gitRemoteAddress: String = rootDir.toString
  override def localDirname: RelPath = rootDir.last
  override def hashDirname: RelPath = s"local:${rootDir.segments.join("-")}"
}

object GitRemote
    extends GitThriftParser[RepoLocation, GitRemote]
    with GitStringParser[GitRemote] {
  override def apply(location: RepoLocation): ThriftParse = asThriftParse("git remote", location,
    location.backendLocationSpec
      .derefOptionalField("repo location specification")
      .flatMap(GitRemote(_).asTry))

  // TODO: we would do any parsing of `backendLocationSpec` (e.g. into a url, file path, etc) here
  // and return a different implementor of `GitRemote` for different formats of inputs.
  // Currently, we interpret every string as pointing to a local directory path.
  override def apply(locationSpec: String): StringParse = asStringParse(
    "location specification", locationSpec,
    Return(LocalFilesystemRepo(Path(locationSpec))))
}

case class GitWorktreeBase(dir: Directory)

case class GitRepoParams(cloneBase: GitCloneBase, worktreeBase: GitWorktreeBase)

case class GitCheckedOutWorktree(
  cloneResult: GitCloneResultLocalDirectory,
  revision: GitRevision,
  dir: Directory)
    extends Thriftable[Checkout] {
  override def asThrift = {
    val checkoutLocation = CheckoutLocation(Some(dir.asStringPath))
    Checkout(Some(checkoutLocation), Some(cloneResult.source.asThrift), Some(revision.asThrift))
  }
}

case class GitCheckoutRequest(source: GitRemote, revision: GitRevision)
    extends Thriftable[CheckoutRequest] {
  override def asThrift = CheckoutRequest(Some(source.asThrift), Some(revision.asThrift))

  def hashDirname: RelPath = s"${source.hashDirname}@${revision.asCanonicalString}"

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
    // FIXME: this should be unique per collaboration (???)
    val worktreeDirCloned = Directory.maybeExistingDir(intoWorktreeDir).flatMap {
      case Some(existingWorktree) => Future(existingWorktree)
      case None => Process(Seq(
        "git", "worktree", "add", intoWorktreeDir.asStringPath, revision.asCommandLineArg),
        cwd = cloneResult.dir.asFile)
          .executeForOutput
          .flatMap(_ => Directory(intoWorktreeDir))
    }

    // Verify that the checked-out worktree is at the right revision.
    worktreeDirCloned.flatMap { checkedOutWorktreeDir =>
      Process(Seq("git", "rev-parse", "HEAD"), cwd = checkedOutWorktreeDir.asFile)
        .executeForOutput
        .flatTry { case OutputStrings(stdout, _) => {
          val repoRevision = stdout.trim
          val expectedRevision = revision.asCanonicalString
          if (repoRevision == expectedRevision) {
            Return(GitCheckedOutWorktree(cloneResult, revision, checkedOutWorktreeDir))
          } else {
            val errMsg = (
              s"Worktree directory ${checkedOutWorktreeDir} did not point to " +
                s"expected revision ${revision} (was: ${repoRevision}).")
            Throw(GitCacheError(errMsg))
          }
        }}
    }
  }

  private def createWorktreeForRevision(
    cloneResult: GitCloneResultLocalDirectory,
    worktreeBase: GitWorktreeBase): Future[GitCheckedOutWorktree] = {
    getOrCreateWorktreeDir(worktreeBase.dir).flatMap(Directory(_))
      .flatMap(createVerifyWorktreeDir(cloneResult, _))
  }

  def checkout(params: GitRepoParams): Future[GitCheckedOutWorktree] = {
    source.performClone(params.cloneBase)
      .flatMap(createWorktreeForRevision(_, params.worktreeBase))
  }
}

object GitCheckoutRequest extends GitThriftParser[CheckoutRequest, GitCheckoutRequest] {
  // TODO: this could be a `StringParser` as well?
  override def apply(request: CheckoutRequest) = {
    val source = request.source.derefOptionalField("source").flatMap(GitRemote(_).asTry)
    val revision = request.revision.derefOptionalField("revision").flatMap(GitRevision(_).asTry)

    val checkReq = source.join(revision).map { case (src, rev) => GitCheckoutRequest(src, rev) }
    asThriftParse("checkout request", request, checkReq)
  }
}

case class GitIdentificationError(message: String) extends GitError(message)

// TODO: what other notions of identity should we consider? This is probably fine for a first draft.
case class GitUser(email: String) extends Thriftable[UserId] with HasCanonicalString {
  override def asThrift = UserId(Some(email))
  override def asCanonicalString = email
}

object GitUser
    extends GitThriftParser[UserId, GitUser]
    with GitStringParser[GitUser] {
  // From https://stackoverflow.com/a/13087910/2518889 -- should use a more legitimate solution, but
  // email as the sole identifier should be revised anyway.
  val emailRegex = rx"\A([0-9a-zA-Z](?>[-.\w]*[0-9a-zA-Z])*@(?>[0-9a-zA-Z][-\w]*[0-9a-zA-Z]\.)+[a-zA-Z]{2,9})\Z"

  override def apply(gitId: UserId) = asThriftParse("git user id", gitId,
    gitId.uid.derefOptionalField("uid").flatMap(GitUser(_).asTry))

  override def apply(email: String) = asStringParse("email address", email,
    emailRegex.findFirstIn(email) match {
      case Some(validEmail) => Return(new GitUser(validEmail))
      case None => Throw(GitIdentificationError(
        s"invalid email ${email}: email address must match ${emailRegex}"))
    })
}

case class GitLocationParseError(message: String) extends GitError(message)

case class GitLineRange(beg: Int, end: Int) extends Thriftable[LineRangeForFile] {
  def asThrift = LineRangeForFile(Some(beg), Some(end))
}

object GitLineRange extends GitThriftParser[LineRangeForFile, GitLineRange] {
  override def apply(range: LineRangeForFile) = asThriftParse("line range", range, {
    val rangeBegin = range.beginning.derefOptionalField("beginning of line range")
      .flatMap(beg => if (beg >= 1) Return(beg) else Throw(GitLocationParseError(
        s"beginning of line range ${beg} must be >= 1")))
    rangeBegin.flatMap { beg => range.ending.derefOptionalField("end of line range")
      .flatMap(end => if (end >= beg) Return(end) else Throw(GitLocationParseError(
        s"end of line range ${end} must be >= the begining ${beg}")))
      .map(GitLineRange(beg, _))
    }
  })
}

// This is intentionally a file *path*, not necessarily a file that exists in any given repo (yet).
case class GitRepoFile(pathFromRepoRoot: RelPath) extends Thriftable[RepoFile] {
  override def asThrift = RepoFile(Some(pathFromRepoRoot.asStringPath))
}

object GitRepoFile extends GitThriftParser[RepoFile, GitRepoFile] {
  override def apply(repoFile: RepoFile) = asThriftParse("repo file", repoFile,
    repoFile.fileRelativePath.derefOptionalField("file relative path")
      .map(RelPath(_))
      .map(GitRepoFile(_)))
}

case class GitFileWithRange(repoFile: GitRepoFile, range: Option[GitLineRange])
    extends Thriftable[FileWithRange] {
  override def asThrift = FileWithRange(Some(repoFile.asThrift), range.map(_.asThrift))
}

object GitFileWithRange extends GitThriftParser[FileWithRange, GitFileWithRange] {
  override def apply(fileWithRange: FileWithRange) = asThriftParse(
    "file with range", fileWithRange, {
      val repoFile = fileWithRange.file.derefOptionalField("file").flatMap(GitRepoFile(_).asTry)
      val lineRange = fileWithRange.lineRangeInFile.map(GitLineRange(_).asTry).flipTryOpt

      repoFile.join(lineRange).map { case (file, rangeOpt) => GitFileWithRange(file, rangeOpt) }
    })
}

case class GitHunk(range: GitFileWithRange) extends Thriftable[Hunk] {
  override def asThrift = Hunk(Some(range.asThrift))
}

object GitHunk extends GitThriftParser[Hunk, GitHunk] {
  override def apply(hunk: Hunk) = asThriftParse("hunk", hunk,
    hunk.fileWithRange.derefOptionalField("fileWithRange")
      .flatMap(GitFileWithRange(_).asTry.map(GitHunk(_))))
}

case class GitNotesPingLocation(hunks: Seq[GitHunk]) extends Thriftable[PingLocation] {
  override def asThrift = PingLocation(Some(hunks.map(_.asThrift)))
}

object GitNotesPingLocation extends GitThriftParser[PingLocation, GitNotesPingLocation] {
  override def apply(pingLocation: PingLocation) = asThriftParse("ping location", pingLocation,
    pingLocation.hunkCollection.derefOptionalField("hunkCollection").flatMap {
      case Seq() => Throw(GitLocationParseError(
        s"invalid ping location ${pingLocation}: a non-empty set of hunks must be provided"))
      case hunks => hunks.map(GitHunk(_).asTry)
          .collectTries
          .map(GitNotesPingLocation(_))
    })
}

case class GitNotesSourceParseError(message: String) extends GitError(message)

sealed trait GitNotesPingSource extends Thriftable[PingSource]

case class GitNotesRegionComment(pingLocation: GitNotesPingLocation) extends GitNotesPingSource {
  override def asThrift = PingSource.RegionComment(
    RegionComment(Some(pingLocation.asThrift)))
}

object GitNotesRegionComment extends GitThriftParser[PingSource, GitNotesPingSource] {
  // FIXME: make it so we can do the actual specific union type only here
  override def apply(regSrc: PingSource) = asThriftParse("region comment", regSrc, regSrc match {
    case PingSource.RegionComment(regCom) => regCom.pingLocation.derefOptionalField("ping location")
        .flatMap(GitNotesPingLocation(_).asTry.map(GitNotesRegionComment(_)))
    case _ => Throw(GitNotesSourceParseError(
      s"internal error: ping source ${regSrc} must be a region comment"))
  })
}

object GitNotesThreadComment extends GitNotesPingSource {
  override def asThrift = PingSource.ThreadComment(ThreadComment())
}

case class GitPingParseError(message: String) extends GitError(message)

case class GitNotesPingId(gitObj: GitObjectHash)
    extends Thriftable[PingId]
    with GitCommandArg {
  override def asThrift = PingId(Some(asCanonicalString))
  override def asCanonicalString: String = gitObj.asCanonicalString
}

object GitNotesPingId
    extends GitThriftParser[PingId, GitNotesPingId]
    with GitStringParser[GitNotesPingId] {
  override def apply(pingId: PingId) = asThriftParse("ping id", pingId,
    pingId.pid.derefOptionalField("pid")
      .flatMap(GitNotesPingId(_).asTry))
  override def apply(pingIdSpec: String) = asStringParse("ping id specification", pingIdSpec,
    GitObjectHash(pingIdSpec).asTry.map(GitNotesPingId(_)))
}

case class GitNotesPinnedPingId(pingId: GitNotesPingId, rev: GitRevision)

case class GitNotesReply(parent: GitNotesPingId) extends GitNotesPingSource {
  override def asThrift = PingSource.Reply(Reply(Some(parent.asThrift)))
}

object GitNotesReply extends GitThriftParser[PingSource, GitNotesPingSource] {
  // FIXME: make it so we can do the actual specific union type only here
  override def apply(replySrc: PingSource) = asThriftParse("reply", replySrc, replySrc match {
    case PingSource.Reply(reply) => reply.parent.derefOptionalField("parent")
        .flatMap(GitNotesPingId(_).asTry.map(GitNotesReply(_)))
    case _ => Throw(GitNotesSourceParseError(
      s"internal error: ping source ${replySrc} must be a reply to another ping"))
  })
}

object GitNotesPingSource extends GitThriftParser[PingSource, GitNotesPingSource] {
  override def apply(pingSource: PingSource) = asThriftParse("ping source", pingSource,
    pingSource match {
      case regCom: PingSource.RegionComment => GitNotesRegionComment(regCom).asTry
      case _: PingSource.ThreadComment => Return(GitNotesThreadComment)
      case reply: PingSource.Reply => GitNotesReply(reply).asTry
      case field: PingSource.UnknownUnionField => Throw(GitLocationParseError(
        s"unknown union field ${field}"))
    })
}

case class GitNotesPing(
  source: GitNotesPingSource,
  notifies: TargetSpecification,
  approves: ApprovalSpecification,
  author: GitUser,
  body: String,
) extends Thriftable[Ping] {
  override def asThrift = Ping(
    Some(source.asThrift),
    Some(notifies),
    Some(approves),
    Some(author.asThrift),
    Some(body),
  )
}

object GitNotesPing extends GitThriftParser[Ping, GitNotesPing] {
  override def apply(ping: Ping) = asThriftParse("ping", ping,
    ping match {
      case Ping(
        Some(sourceThrift), Some(notifies), Some(approves), Some(authorThrift), Some(body)) => {
        GitNotesPingSource(sourceThrift).asTry.join(GitUser(authorThrift).asTry)
          .map { case (source, author) => GitNotesPing(source, notifies, approves, author, body) }
      }
      case _ => Throw(GitThriftParseError(
        "one or more fields were not provided (all are required for git)"))
    })
}

case class GitNotesPinnedPing(ping: GitNotesPing, rev: GitRevision) extends Thriftable[PinnedPing] {
  override def asThrift = PinnedPing(Some(ping.asThrift), Some(rev.asThrift))
}

case class GitNotesPingEntry(pingId: GitNotesPingId, ping: GitNotesPinnedPing) {
  def asThriftMapTuple = (pingId.asThrift -> ping.asThrift)
}

case class GitNotesPingCollection(pingEntries: Seq[GitNotesPingEntry])
    extends Thriftable[PingCollection] {
  private def asPingMap = pingEntries.map(_.asThriftMapTuple).toMap
  override def asThrift = PingCollection(Some(asPingMap))
}

case class GitRevisionRange(begin: GitRevision, end: GitRevision)
    extends Thriftable[RevisionRange]
    with GitCommandArg {
  override def asCanonicalString: String = s"${begin.asCanonicalString}..${end.asCanonicalString}"
  override def asThrift = RevisionRange(Some(asCanonicalString))
}

object GitRevisionRange
    extends GitThriftParser[RevisionRange, GitRevisionRange]
    with GitStringParser[GitRevisionRange] {
  val maxLengthShaRange = rx"\A([0-9a-f]{40})\.\.([0-9a-f]{40})\Z"

  override def apply(range: RevisionRange) = asThriftParse("revision range", range,
    range.backendRevisionRangeSpec.derefOptionalField("git revision range")
      .flatMap(GitRevisionRange(_).asTry))

  override def apply(rangeSpec: String) = asStringParse("revision range specification", rangeSpec,
    rangeSpec match {
      case maxLengthShaRange(begin, end) => GitRevision(begin).asTry.join(GitRevision(end).asTry)
          .map { case (beg, end) => GitRevisionRange(beg, end) }
      case _ => Throw(GitInputParseError(s"range must match ${maxLengthShaRange}"))
    })
}

case class GitNotesListParseError(message: String) extends GitError(message)

case class GitCheckoutPingSpan(checkout: GitCheckedOutWorktree, range: GitRevisionRange) {
  import GitCheckoutPingSpan._

  private def allRevisionsInRange: Future[Seq[GitRevision]] =
    Process(
      Seq("git", "log", "--pretty=format:%H", range.asCommandLineArg),
      cwd = checkout.dir.asFile)
    .executeForOutput
    .flatTry(_.stdoutLines.map(GitRevision(_).asTry).collectTries)

  private def getNotesObjects(
    revsInRange: Set[GitRevision],
    lines: Seq[String]
  ): Try[Seq[GitNotesPinnedPingId]] = {
    val notesTries: Seq[Try[Option[GitNotesPinnedPingId]]] = lines.map {
      case "" => Return(None)
      case notesListLine(notesRef, commitRef) => {
        GitObjectHash(notesRef).asTry.join(GitRevision(commitRef).asTry).map {
          case (notesObj, commitRev) => revsInRange(commitRev) match {
            case true => Some(GitNotesPinnedPingId(GitNotesPingId(notesObj), commitRev))
            case false => None
          }
        }
      }
      case line => Throw(GitNotesListParseError(
        s"line ${line} of git notes list output could not be parsed: " +
          s"must match ${GitCheckoutPingSpan.notesListLine}"))
    }
    notesTries.collectTries.map(_.flatten)
  }

  private def pingEntryForId(pinnedPingId: GitNotesPinnedPingId): Future[GitNotesPingEntry] = {
    Process(Seq("git", "show", pinnedPingId.pingId.asCommandLineArg), cwd = checkout.dir.asFile)
      .executeForThriftStruct[Ping, Ping.type](Ping)
      .flatTry(GitNotesPing(_).asTry)
      .map(p => GitNotesPingEntry(pinnedPingId.pingId, GitNotesPinnedPing(p, pinnedPingId.rev)))
  }

  // TODO: have a notes cache so we don't have to list every note in the current revision range,
  // then parse it as we do here. This is definitely fine for now, though.
  def getPings: Future[GitNotesPingCollection] = allRevisionsInRange.map(_.toSet).flatMap { revs =>
    Process(Seq("git", "notes", "list"), cwd = checkout.dir.asFile)
      .executeForOutput
      .flatTry(out => getNotesObjects(revs, out.stdoutLines))
      .flatMap(_.map(pingEntryForId).collectFutures)
      .map(GitNotesPingCollection(_))
  }
}

object GitCheckoutPingSpan {
  val notesListLine = rx"\A([0-9a-f]{40}) ([0-9a-f]{40})\Z"
}

// TODO: we would probably want to allow more than just a SHA range, otherwise we'd need a new
// checkout every time we fetch from the remote. Allowing an arbitrary ref-like, for both ends of
// the range, should suffice (???).
// Note that git-worktree(1) states that refs are shared across all working trees.
case class GitNotesCollaborationId(source: GitRemote, revisionRange: GitRevisionRange)
    extends Thriftable[CollaborationId]
    with HasCanonicalString {

  override def asCanonicalString = s"${source.asCanonicalString}:${revisionRange.asCanonicalString}"
  override def asThrift = CollaborationId(Some(asCanonicalString))

  def asCheckoutRequest = GitCheckoutRequest(source, revisionRange.end)

  def getCollaboration(params: GitRepoParams): Future[GitNotesCollaboration] = {
    asCheckoutRequest.checkout(params)
      .flatMap { checkout => GitCheckoutPingSpan(checkout, revisionRange)
        .getPings
        .map(GitNotesCollaboration(checkout, _))
      }
  }
}

object GitNotesCollaborationId
    extends GitThriftParser[CollaborationId, GitNotesCollaborationId]
    with GitStringParser[GitNotesCollaborationId] {
  val collabRequestPattern = rx"\A([^:]+):([^:]+)\Z"

  override def apply(collabId: CollaborationId) = asThriftParse("collaboration id", collabId,
    collabId.cid.derefOptionalField("cid")
      .flatMap(GitNotesCollaborationId(_).asTry))

  override def apply(collabSpec: String) = asStringParse("collaboration specification", collabSpec,
    collabSpec match {
      case collabRequestPattern(remoteSpec, rangeSpec) =>
        GitRemote(remoteSpec).asTry.join(GitRevisionRange(rangeSpec).asTry).map {
          case (remote, range) => GitNotesCollaborationId(remote, range)
        }
      case _ => Throw(GitInputParseError(s"spec must match ${collabRequestPattern}"))
    })
}

case class GitRepoCommunicationError(message: String, base: Throwable)
    extends GitError(message, base)

case class GitCollaborationResolutionError(message: String) extends GitError(message)

case class GitNotesCollaboration(checkout: GitCheckedOutWorktree, pings: GitNotesPingCollection)
    extends Thriftable[Collaboration] {
  override def asThrift = Collaboration(Some(checkout.asThrift), Some(pings.asThrift))
}

case class GitNotesCollaborationEntry(
  collabId: GitNotesCollaborationId,
  collab: GitNotesCollaboration,
)

case class GitNotesMatchedCollaborations(collabs: Seq[GitNotesCollaborationEntry])
    extends Thriftable[MatchedCollaborations] {
  override def asThrift = {
    val collabResults = collabs.map {
      case GitNotesCollaborationEntry(cid, collab) => (cid.asThrift -> collab.asThrift)
    }.toMap

    MatchedCollaborations(Some(collabResults))
  }
}

case class GitNotesCollaborationQuery(collabIds: Seq[GitNotesCollaborationId])
    extends Thriftable[CollaborationQuery] {
  def invoke(params: GitRepoParams): Future[GitNotesMatchedCollaborations] = {
    val collabs = collabIds.map { cid =>
      cid.getCollaboration(params).map(GitNotesCollaborationEntry(cid, _))
    }.collectFutures

    collabs.map(GitNotesMatchedCollaborations(_))
  }

  override def asThrift = CollaborationQuery(Some(collabIds.map(_.asThrift)))
}

object GitNotesCollaborationQuery
    extends GitThriftParser[CollaborationQuery, GitNotesCollaborationQuery] {
  override def apply(query: CollaborationQuery) = asThriftParse("collaboration query", query,
    query.collaborationIds.derefOptionalField("collaborationIds").flatMap {
      case Seq() => Throw(GitCollaborationResolutionError(
        "the set of collaboration ids must be non-empty"))
      case collabIds => collabIds.map(GitNotesCollaborationId(_).asTry)
          .collectTries
          .map(GitNotesCollaborationQuery(_))
    })
}

case class GitNotesPinPingRequest(ping: GitNotesPing, rev: GitRevision) extends Thriftable[PinPingRequest] {
  override def asThrift = PinPingRequest(Some(ping.asThrift), Some(rev.asThrift))

  // FIXME: do something smarter than just appending! try merging/etc!
  // FIXME: only return a GitNotesPinnedPing once we actually insert it! call what we have now a
  // GitNotesPinnedPingRequest!
  private def insertPinnedPing(
    pingId: GitNotesPingId, checkout: GitCheckedOutWorktree
  ): Future[GitNotesPingEntry] = {
    val appendNotesArgv = Seq(
      "git", "notes", "append", "-C", pingId.asCommandLineArg, rev.asCommandLineArg)

    Process(appendNotesArgv, cwd = checkout.dir.asFile)
      .executeForOutput.map { _ =>
        val pinnedPing = GitNotesPinnedPing(ping, rev)
        GitNotesPingEntry(pingId, pinnedPing)
      }
  }

  def makePingEntry(checkout: GitCheckedOutWorktree): Future[GitNotesPingEntry] =
    Process(Seq("git", "hash-object", "-w", "--stdin"), cwd = checkout.dir.asFile)
      .executeForOutput(ping.asThrift.toPlaintextStdin)
      .flatTry(output => GitNotesPingId(output.stdout.trim).asTry)
      .flatMap(insertPinnedPing(_, checkout))
}

object GitNotesPinPingRequest extends GitThriftParser[PinPingRequest, GitNotesPinPingRequest] {
  override def apply(pinPingRequest: PinPingRequest) = asThriftParse(
    "pin ping request", pinPingRequest, {
      val wrappedPing = pinPingRequest.ping.derefOptionalField("ping")
        .flatMap(GitNotesPing(_).asTry)
      val wrappedRevision = pinPingRequest.revision.derefOptionalField("revision to pin to")
        .flatMap(GitRevision(_).asTry)
      wrappedPing.join(wrappedRevision).map { case (p, r) => GitNotesPinPingRequest(p, r) }
    })
}

case class GitNotesPublishPingsRequest(
  collabId: GitNotesCollaborationId,
  pingsToPublish: Seq[GitNotesPinPingRequest],
) extends Thriftable[PublishPingsRequest] {
  def publish(params: GitRepoParams): Future[GitNotesPingCollection] = collabId.asCheckoutRequest
    .checkout(params).flatMap { checkout => pingsToPublish
      .map(_.makePingEntry(checkout))
      .collectFutures
      .map(GitNotesPingCollection(_))
    }

  override def asThrift = PublishPingsRequest(
    Some(collabId.asThrift),
    Some(pingsToPublish.map(_.asThrift)))
}

object GitNotesPublishPingsRequest
    extends GitThriftParser[PublishPingsRequest, GitNotesPublishPingsRequest] {
  override def apply(request: PublishPingsRequest) = asThriftParse(
    "publish pings request", request, {
      val collabId = request.collaborationId.derefOptionalField("collaboration id")
        .flatMap(GitNotesCollaborationId(_).asTry)
      val publishPingSet = request.pingsToPin.derefOptionalField("pings to publish")
        .flatMap(_.map(GitNotesPinPingRequest(_).asTry).collectTries)

      collabId.join(publishPingSet).map {
        case (cid, pingsToPin) => GitNotesPublishPingsRequest(cid, pingsToPin)
      }
    })
}
