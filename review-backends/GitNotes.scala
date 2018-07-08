package pingpong.review_backends

import pingpong.io._
import pingpong.parsing.Regex._
import pingpong.protocol.entities._
import pingpong.protocol.pingpong._
import pingpong.protocol.repo_backend._
import pingpong.protocol.review_backend._
import pingpong.repo_backends._
import pingpong.subsystems._

import com.twitter.util.{Try, Return, Throw, Future}

import scala.sys.process._

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
        // Convert to a mapping of `MatchedCollaborations`.
        .map(cid -> _)
    }).map(_.toMap))

    collabRequests.flatMap(reqs => Future.collect(reqs.mapValues(_.getCollaboration(gitRepo))))
      .map(idCollabs => QueryCollaborationsResponse.MatchedCollaborations(idCollabs))
      .rescue { case e => Future {
        QueryCollaborationsResponse.Error(ReviewBackendError(Some(e.toString)))
      }}
  }

  override def publishPings(request: PublishPingsRequest): Future[PublishPingsResponse] = Future {
    PublishPingsResponse.Success(PublishPingsSuccess())
  }
}
