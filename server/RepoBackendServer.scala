package pingpong.server

import pingpong.io._
import pingpong.protocol.repo_backend.{
  CheckoutResponse,
  RepoBackend,
  RepoBackendError,
}
import pingpong.repo_backends._
import pingpong.subsystems.GitRepoParams

import ammonite.ops._
import com.twitter.finatra.thrift._
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.inject.Logging

import javax.inject.Inject

class RepoBackendController @Inject() (params: GitRepoParams)
    extends Controller
    with RepoBackend.ServicePerEndpoint {
  override val getCheckout = handle(RepoBackend.GetCheckout) { args: RepoBackend.GetCheckout.Args =>
    new GitRepoBackend(params).getCheckout(args.request)
  }
}

class RepoBackendServer extends ThriftServer with Logging {
  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .add[RepoBackendController]
  }
}
