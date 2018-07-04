package pingpong.server.tests

import pingpong.protocol.repo_backend.{
  CheckoutRequest,
  CheckoutResponse,
  RepoBackend,
  RepoBackendError,
  Revision
}
import pingpong.server.GitServerMain

import com.twitter.conversions.time._
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}

class ServerFeatureTest extends FeatureTest {
  override val server = new EmbeddedThriftServer(GitServerMain)

  lazy val client =
    server.thriftClient[RepoBackend[Future]](clientId = "client123")

  test("Server#return an error message") {
    Await.result(
      client.getCheckout(
        CheckoutRequest(Some(Revision(Some("???"))), Some(Seq()))),
      2.seconds) should equal(
      CheckoutResponse.Error(RepoBackendError(Some("huh"))))
  }
}
