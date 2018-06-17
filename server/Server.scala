package pingpong.server

import pingpong.protocol.repo_backend.{GetSandboxGlobsResponse, RepoBackend, RepoBackendError}

import com.twitter.finatra.thrift._
import com.twitter.finatra.thrift.routing.ThriftRouter
import com.twitter.inject.Logging
import com.twitter.util.Future

// These become json!

class SimpleController extends Controller with RepoBackend.ServicePerEndpoint {
  override val getSandboxGlobs = handle(RepoBackend.GetSandboxGlobs) {
    args: RepoBackend.GetSandboxGlobs.Args => {
      val request = args.request;
      Future(GetSandboxGlobsResponse.Error(RepoBackendError(Some("huh"))))
    }
  }
}

class Server extends ThriftServer with Logging {
  override def configureThrift(router: ThriftRouter): Unit = {
    router
      .add[SimpleController]
  }
}

object ServerMain extends Server
