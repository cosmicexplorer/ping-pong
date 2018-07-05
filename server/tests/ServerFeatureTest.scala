package pingpong.server.tests

import pingpong.protocol.repo_backend._
import pingpong.server.RepoBackendServerMain

import com.twitter.conversions.time._
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.server.FeatureTest
import com.twitter.util.{Await, Future}

class ServerFeatureTest extends FeatureTest {
  override val server = new EmbeddedThriftServer(RepoBackendServerMain)

  lazy val client =
    server.thriftClient[RepoBackend[Future]](clientId = "client123")

  // FIXME: make this highlight correctly in ensime!
  test("Server#return an error message") {
    Await.result(
      client.getCheckout(
        CheckoutRequest(Some(RepoLocation(Some("???"))), Some(Revision(Some("a"))))),
      2.seconds) should equal(
      CheckoutResponse.Error(RepoBackendError(Some("huh"))))
  }
}
