package pingpong.server

import pingpong.protocol.repo_backend._
import pingpong.protocol.review_backend._
import pingpong.repo_backends._
import pingpong.review_backends._

import com.twitter.finatra.thrift._
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.inject.Logging

import javax.inject.Inject

class RepoBackendController @Inject() (repoBackend: GitRepoBackend)
    extends Controller
    with RepoBackend.ServicePerEndpoint {
  override val getCheckout = handle(RepoBackend.GetCheckout) { args =>
    repoBackend.getCheckout(args.request)
  }
}

class RepoBackendServer extends ThriftServer with Logging {
  override def configureThrift(router: ThriftRouter): Unit = router.add[RepoBackendController]
}

class ReviewBackendController @Inject() (reviewBackend: GitNotesReviewBackend)
    extends Controller
    with ReviewBackend.ServicePerEndpoint {
  override val queryCollaborations = handle(ReviewBackend.QueryCollaborations) { args =>
    reviewBackend.queryCollaborations(args.query)
  }

  override val publishPings = handle(ReviewBackend.PublishPings) { args =>
    reviewBackend.publishPings(args.request)
  }
}

class ReviewBackendServer extends ThriftServer with Logging {
  override def configureThrift(router: ThriftRouter): Unit = router.add[ReviewBackendController]
}
