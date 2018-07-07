package pingpong.review_backends

import pingpong.parsing.Regex._
import pingpong.protocol.entities._
import pingpong.protocol.pingpong._
import pingpong.protocol.repo_backend._
import pingpong.protocol.review_backend._
import pingpong.repo_backends._

import com.twitter.util.{Try, Return, Throw, Future}

class GitNotesError(message: String, base: Throwable) extends RuntimeException(message, base) {
  def this(message: String) = this(message, null)
}

case class GitIdentificationError(message: String) extends GitNotesError(message)

case class GitRevParseError(message: String) extends GitNotesError(message)

case class GitCollaborationResolutionError(message: String) extends GitNotesError(message)

case class GitRepoCommunicationError(message: String, base: RepoBackendError)
    extends GitNotesError(message, base)

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

case class GitRevisionRange(begin: GitRevision, end: GitRevision) {
  def asThrift = RevisionRange(Some(s"${begin.sha}..${end.sha}"))
}

object GitRevisionRange {
  val maxLengthShaRange = rx"\A([0-9a-f]{40})\.\.([0-9a-f]{40})\Z"

  def apply(range: RevisionRange): Try[GitRevisionRange] = range.backendRevisionRangeSpec match {
    case Some(rangeSpec) => GitRevisionRange(rangeSpec)
    case None => Throw(GitInputParseError(
      s"invalid revision range ${range}: range must be provided"))
  }

  def apply(rangeSpec: String): Try[GitRevisionRange] = rangeSpec match {
    case maxLengthShaRange(begin, end) => {
      val begRev = GitRevision(Revision(Some(begin)))
      val endRev = GitRevision(Revision(Some(end)))
      begRev.flatMap(b => endRev.map(GitRevisionRange(b, _)))
    }
    case _ => Throw(GitRevParseError(
      s"invalid revision range ${rangeSpec}: range must match ${maxLengthShaRange}"))
  }
}

case class GitNotesCollaborationRequest(source: GitRemote, revisionRange: GitRevisionRange) {
  def asCheckoutRequest = GitCheckoutRequest(source, revisionRange.end)
}

object GitNotesCollaborationRequest {
  val collabRequestPattern = rx"\A([^:]+):([^:]+)\Z"

  def apply(collabId: CollaborationId): Try[GitNotesCollaborationRequest] = collabId.cid match {
    case Some(collabRequestPattern(remoteSpec, rangeSpec)) => {
      val remote = GitRemote(remoteSpec)
      val range = GitRevisionRange(rangeSpec)
      remote.flatMap(src => range.map(GitNotesCollaborationRequest(src, _)))
    }
    case _ => Throw(GitInputParseError(
      s"invalid collaboration id ${collabId}: id must be provided and match ${collabRequestPattern}"
    ))
  }
}

class GitNotesReviewBackend(gitRepo: GitRepoBackend) extends ReviewBackend.MethodPerEndpoint {
  def pingsForCollaboration(
    request: GitNotesCollaborationRequest,
    checkout: Checkout
  ): Future[PingCollection] = Future(PingCollection(Some(Map.empty)))

  override def queryCollaborations(
    query: CollaborationQuery
  ): Future[QueryCollaborationsResponse] = {
    val collabIds = query.collaborationIds.getOrElse(Seq()) match {
      case Seq() => Future.const(Throw(GitCollaborationResolutionError(
        s"invalid collaboration query ${query}: " +
          "a non-empty set of collaboration ids must be provided"
      )))
      case x => Future(x)
    }
    // Collect the `Try`s and fail early if parsing any of the collaboration requests fails.
    val collabRequests = collabIds.flatMap(ids => Future.collect(ids.map { cid =>
      Future.const(GitNotesCollaborationRequest(cid))
        .map((cid, _))
    }))
    val collabResults = collabRequests.flatMap(reqs => Future.collect(reqs.map {
      case (cid, collabReq) => gitRepo.getCheckout(collabReq.asCheckoutRequest.asThrift).flatMap {
        case CheckoutResponse.Error(err) => Future.const(Throw(
          GitRepoCommunicationError(s"error checking out ${cid}", err)))
        case x: CheckoutResponse.UnknownUnionField => Future.const(Throw(
          GitCollaborationResolutionError(
            s"error checking out ${cid}: unknown union for checkout response ${x}")
        ))
        case CheckoutResponse.Completed(checkout) => pingsForCollaboration(collabReq, checkout)
            .map(pings => (cid, Collaboration(Some(checkout), Some(pings))))
      }
    }))

    collabResults
      .map(idCollabs => QueryCollaborationsResponse.MatchedCollaborations(idCollabs.toMap))
      .rescue { case e => Future {
        QueryCollaborationsResponse.Error(ReviewBackendError(Some(e.toString)))
      }}
  }

  override def publishPings(request: PublishPingsRequest): Future[PublishPingsResponse] = Future {
    PublishPingsResponse.Success(PublishPingsSuccess())
  }
}
