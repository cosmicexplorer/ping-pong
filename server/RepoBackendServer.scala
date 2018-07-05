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

// class RepoBackendController extends Controller with RepoBackend.MethodPerEndpoint {
//   override val getCheckout = handle(RepoBackend.GetCheckout) {
//     args: RepoBackend.GetCheckout.Args =>
//       {
//         // val request = args.request;
//         Future(CheckoutResponse.Error(RepoBackendError(Some("huh"))))
//       }
//   }
// }

// class RepoBackendServer[T <: RepoBackend.MethodPerEndpoint] extends ThriftServer with Logging {
//   override def configureThrift(router: ThriftRouter): Unit = {
//     router
//       .add[RepoBackendController[T]]
//   }
// }

// object GitServerMain extends RepoBackendServer[GitRepoBackend]
