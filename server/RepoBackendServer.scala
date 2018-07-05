package pingpong.server

import pingpong.protocol.repo_backend.{
  CheckoutResponse,
  RepoBackend,
  RepoBackendError,
}
import pingpong.repo_backends.GitRepoBackend

import com.twitter.finatra.thrift._
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.inject.Logging
import com.twitter.util.Future

class RepoBackendController extends Controller with RepoBackend.ServicePerEndpoint {
  override val getCheckout = handle(RepoBackend.GetCheckout) {
    args: RepoBackend.GetCheckout.Args =>
      {
        // val request = args.request;
        Future(CheckoutResponse.Error(RepoBackendError(Some("huh"))))
      }
  }
}

class RepoBackendServer extends ThriftServer with Logging {
  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .add[RepoBackendController]
  }
}

object RepoBackendServerMain extends RepoBackendServer
