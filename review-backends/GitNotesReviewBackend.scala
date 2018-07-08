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

class GitNotesReviewBackend(repoParams: GitRepoParams) extends ReviewBackend.MethodPerEndpoint {
  def pingsForCollaboration(
    request: GitNotesCollaborationRequest,
    checkout: Checkout
  ): Future[PingCollection] = Future(PingCollection(Some(Map.empty)))

  override def queryCollaborations(
    query: CollaborationQuery
  ): Future[QueryCollaborationsResponse] = {
    Future.const(GitNotesCollaborationQuery(query))
      .flatMap(_.invoke(repoParams))
      .rescue { case e => Future {
        QueryCollaborationsResponse.Error(ReviewBackendError(Some(e.toString)))
      }}
  }

  override def publishPings(request: PublishPingsRequest): Future[PublishPingsResponse] = Future {
    PublishPingsResponse.Success(PublishPingsSuccess())
  }
}
