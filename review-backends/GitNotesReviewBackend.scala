package pingpong.review_backends

import pingpong.util.ErrorExt._
import pingpong.util.FutureTryExt._
import pingpong.protocol.review_backend._
import pingpong.subsystems._

import com.twitter.util.Future

class GitNotesReviewBackend(repoParams: GitRepoParams) extends ReviewBackend.MethodPerEndpoint {
  override def queryCollaborations(
    query: CollaborationQuery
  ): Future[QueryCollaborationsResponse] = GitNotesCollaborationQuery(query).asTry
    .constFuture
    .flatMap(_.invoke(repoParams).map { matchedCollabs =>
      QueryCollaborationsResponse.MatchedCollaborations(matchedCollabs.asThrift)
    })
    .rescue { case e => Future {
      QueryCollaborationsResponse.Error(ReviewBackendError(Some(e.asStackTrace)))
    }}

  override def publishPings(
    request: PublishPingsRequest
  ): Future[PublishPingsResponse] = GitNotesPublishPingsRequest(request).asTry
    .constFuture
    .flatMap(_.publish(repoParams).map(ps => PublishPingsResponse.PublishedPings(ps.asThrift)))
    .rescue { case e => Future {
      PublishPingsResponse.Error(new ReviewBackendError(Some(e.asStackTrace)))
    }}
}
