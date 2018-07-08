package pingpong.review_backends

import pingpong.io.FutureTryExt._
import pingpong.protocol.review_backend._
import pingpong.subsystems._

import com.twitter.util.Future

class GitNotesReviewBackend(repoParams: GitRepoParams) extends ReviewBackend.MethodPerEndpoint {
  override def queryCollaborations(
    query: CollaborationQuery
  ): Future[QueryCollaborationsResponse] = GitNotesCollaborationQuery(query)
    .constFuture
    .flatMap(_.invoke(repoParams).map { matchedCollabs =>
      QueryCollaborationsResponse.MatchedCollaborations(matchedCollabs.asThrift)
    })
    .rescue { case e => Future {
      QueryCollaborationsResponse.Error(ReviewBackendError(Some(e.toString)))
    }}

  override def publishPings(
    request: PublishPingsRequest
  ): Future[PublishPingsResponse] = GitNotesPublishPingsRequest(request)
    .constFuture
    .flatMap(_.publish(repoParams).map(pings => PublishPingsResponse.PublishedPings(pings.asThrift)))
    .rescue { case e => Future {
      PublishPingsResponse.Error(ReviewBackendError(Some(e.toString)))
    }}
}
