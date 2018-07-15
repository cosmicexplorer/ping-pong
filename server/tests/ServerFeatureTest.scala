package pingpong.server.tests

import pingpong.io._
import pingpong.protocol.entities._
import pingpong.protocol.notifications._
import pingpong.protocol.pingpong._
import pingpong.protocol.repo_backend._
import pingpong.protocol.review_backend._
import pingpong.repo_backends._
import pingpong.review_backends._
import pingpong.server._
import pingpong.subsystems._

import ammonite.ops._
import com.google.inject.{Provides, Singleton}
import com.twitter.bijection.Conversion.asMethod
import com.twitter.bijection.twitter_util.UtilBijections._
import com.twitter.finatra.thrift.EmbeddedThriftServer
import com.twitter.inject.TwitterModule
import com.twitter.inject.server.FeatureTestMixin
import com.twitter.util.{Await, Future}
import org.scalatest._

object TestEnvironmentModule extends TwitterModule {
  lazy val curRepoRoot = Path(%%("git", "rev-parse", "--show-toplevel")(pwd).out.trim)
  lazy val prevSha = %%("git", "rev-parse", "HEAD~1")(pwd).out.trim
  lazy val curSha = %%("git", "rev-parse", "HEAD")(pwd).out.trim

  lazy val tempDirBase = tmp.dir(deleteOnExit = true)

  @Singleton
  @Provides
  def providesGitRepoParams: GitRepoParams = {
    val gitCloneBase = tempDirBase / "clones"
    mkdir! gitCloneBase
    val gitCheckoutBase = tempDirBase / "checkouts"
    mkdir! gitCheckoutBase
    val paramsResult = Future.join(Directory(gitCloneBase), Directory(gitCheckoutBase))
      .map { case (cloneDir, checkoutDir) =>
        GitRepoParams(GitCloneBase(cloneDir), GitWorktreeBase(checkoutDir)) }
    Await.result(paramsResult)
  }

  @Singleton
  @Provides
  def providesGitRepoBackend(params: GitRepoParams): GitRepoBackend = new GitRepoBackend(params)

  @Singleton
  @Provides
  def providesGitNotesReviewBackend(params: GitRepoParams): GitNotesReviewBackend =
    new GitNotesReviewBackend(params)
}

abstract class AsyncTwitterFeatureTest extends AsyncFunSuite with FeatureTestMixin {
  // `AsyncFunSuite` requires a scala `Future`, so we convert it to one here with the bijection lib.
  def testAsync(msg: String)(f: => Future[org.scalatest.compatible.Assertion]) =
    // FIXME: make this file highlight correctly in ensime!
    // NB: these failures also occur when using `./pants repl`!
    test(msg)(f.as[scala.concurrent.Future[org.scalatest.compatible.Assertion]])
}

class RepoBackendServerFeatureTest extends AsyncTwitterFeatureTest {
  import TestEnvironmentModule._

  override val server = new EmbeddedThriftServer(
    twitterServer = new RepoBackendServer {
      override def overrideModules = Seq(TestEnvironmentModule)
    })

  lazy val repoClient = server.thriftClient[RepoBackend[Future]](clientId = "repo-client")

  testAsync("Server#perform a git checkout") {
    val request = CheckoutRequest(
      Some(RepoLocation(Some(curRepoRoot.toString))),
      Some(Revision(Some(curSha))))

    repoClient.getCheckout(request)
      .map { case CheckoutResponse.Completed(Checkout(Some(checkout), Some(repo), Some(rev))) =>
        // TODO: is it kosher to use logic from the `GitRemote` wrapper in this test? Should this be
        // more "from scratch"?
        val remote = GitRemote(repo).asTry.get
        val middleDirname = s"${remote.hashDirname}@${rev.backendRevisionSpec.get}"
        val checkoutsDirUnderTmp = (
          tempDirBase /
            "checkouts" /
            RelPath(middleDirname) /
            remote.localDirname)
        Path(checkout.sandboxRootAbsolutePath.get) should equal(checkoutsDirUnderTmp)
        repo should equal(RepoLocation(Some(curRepoRoot.toString)))
        rev should equal(Revision(Some(curSha)))

        // TODO: do a few more git process invokes, check to see if the checkout is correct
      }
  }
}

class ReviewBackendServerFeatureTest extends AsyncTwitterFeatureTest {
  import TestEnvironmentModule._

  override val server = new EmbeddedThriftServer(
    twitterServer = new ReviewBackendServer {
      override def overrideModules = Seq(TestEnvironmentModule)
    })

  lazy val reviewClient = server.thriftClient[ReviewBackend[Future]](clientId = "review-client")

  testAsync("Server#publish and query some pings") {
    val emptyPing = Ping(
      Some(PingSource.ThreadComment(ThreadComment())),
      Some(TargetSpecification(Some(Seq()))),
      Some(ApprovalSpecification(Some(Seq()))),
      Some(UserId(Some("someone@example.com"))),
      Some("some text"))

    val pinPingRequest = PinPingRequest(Some(emptyPing), Some(Revision(Some(curSha))))

    val expectedPinnedPing = PinnedPing(Some(emptyPing), Some(Revision(Some(curSha))))

    val collabId = CollaborationId(Some(s"${curRepoRoot.toString}:${prevSha}..${curSha}"))

    val publishRequest = PublishPingsRequest(Some(collabId), Some(Seq(pinPingRequest)))

    val writtenPingId = reviewClient.publishPings(publishRequest).map {
      case PublishPingsResponse.PublishedPings(PingCollection(Some(pingMap))) =>
        pingMap.toSeq match {
          case Seq((pingId, returnedPinnedPing)) => {
            returnedPinnedPing should equal(expectedPinnedPing)
            pingId
          }
        }
    }

    val collabQuery = CollaborationQuery(Some(Seq(collabId)))

    writtenPingId.flatMap { pingId => reviewClient.queryCollaborations(collabQuery).map {
      case QueryCollaborationsResponse.MatchedCollaborations(
        MatchedCollaborations(Some(collabMap))) => {
        val Seq((returnedCollabId, collab)) = collabMap.toSeq
        returnedCollabId should equal(collabId)

        // TODO: verify the collaboration's Checkout
        val Seq((returnedPingId, returnedPinnedPing)) = collab.pings.get.pingMap.get.toSeq
        returnedPingId should equal(pingId)
        returnedPinnedPing should equal(expectedPinnedPing)
      }
    }}
  }

}
