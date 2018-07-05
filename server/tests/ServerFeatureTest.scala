package pingpong.server.tests

import pingpong.io._
import pingpong.repo_backends._
import pingpong.protocol.repo_backend._
import pingpong.server.RepoBackendServer

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
  lazy val tempDirBase = tmp.dir()

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
}

class ServerFeatureTest extends AsyncFunSuite with FeatureTestMixin {
  override val server = new EmbeddedThriftServer(
    twitterServer = new RepoBackendServer {
      override def overrideModules = Seq(TestEnvironmentModule)
  })

  lazy val client =
    server.thriftClient[RepoBackend[Future]](clientId = "client123")

  lazy val curRepoRoot = Path(%%("git", "rev-parse", "--show-toplevel")(pwd).out.trim)
  lazy val curSha = %%("git", "rev-parse", "HEAD")(pwd).out.trim

  // FIXME: make this highlight correctly in ensime!
  test("Server#return an error message") {
    client.getCheckout(
      CheckoutRequest(
        Some(RepoLocation(Some(curRepoRoot.toString))),
        Some(Revision(Some(curSha)))))
      .map { case CheckoutResponse.Completed(Checkout(Some(checkout), Some(repo), Some(rev))) =>
        val remote = GitRemote(repo).get
        val middleDirname = s"${remote.hashDirname}@${rev.backendRevisionSpec.get}"
        val checkoutsDirUnderTmp = (
          TestEnvironmentModule.tempDirBase /
            "checkouts" /
            RelPath(middleDirname) /
            remote.localDirname)
        Path(checkout.sandboxRootAbsolutePath.get) should equal(checkoutsDirUnderTmp)
        repo should equal(RepoLocation(Some(curRepoRoot.toString)))
        rev should equal(Revision(Some(curSha)))
      }
      .as[scala.concurrent.Future[org.scalatest.compatible.Assertion]]
  }
}
