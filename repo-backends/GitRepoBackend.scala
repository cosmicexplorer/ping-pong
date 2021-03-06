package pingpong.repo_backends

import pingpong.util.ErrorExt._
import pingpong.util.FutureTryExt._
import pingpong.protocol.repo_backend._
import pingpong.subsystems._

// import com.twitter.bijection._
// import com.twitter.bijection.Conversion.asMethod
import com.twitter.util.Future

class GitRepoBackend(repoParams: GitRepoParams) extends RepoBackend.MethodPerEndpoint {
  // Use worktrees from a single clone, instead of doing a new clone each time. Keep a base clone of
  // each remote in `repoParams.cloneBase`, then keep each checkout in a worktree branched from that
  // clone, under `repoParams.worktreeBase`, using directory paths created from the hash of the
  // request. If the hash matches, we check that the checkout actually corresponds to the request!
  // TODO: cleanup LRU checkouts after we take up enough space!
  override def getCheckout(request: CheckoutRequest): Future[CheckoutResponse] = {
    // TODO: use these!
    // val wrappedRequest = request.as[GitCheckoutRequest]
    val wrappedRequest = GitCheckoutRequest(request)
    val checkoutExecution = wrappedRequest.asTry
      .constFuture
      .flatMap(_.checkout(repoParams))

    checkoutExecution
      .map(checkout => CheckoutResponse.Completed(checkout.asThrift))
      .rescue { case e => Future(CheckoutResponse.Error(
        RepoBackendError(Some(e.asStackTrace))))
      }
  }
}
