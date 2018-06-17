package pingpong.server

import com.twitter.finatra.http._
import com.twitter.finatra.http.filters.CommonFilters
import com.twitter.finatra.http.routing.HttpRouter
import com.twitter.finatra.request.FormParam
import com.twitter.finatra.validation.NotEmpty;
import com.twitter.inject.Logging

case class MyRequest(@NotEmpty @FormParam text: String)
case class MyResponse(text: String)

class SimpleController extends Controller {
  post("/") { request: MyRequest => MyResponse(s"request text ${}") }
}

class Server extends HttpServer with Logging {
  override def configureHttp(router: HttpRouter): Unit = {
    router
      .filter[CommonFilters]
      .add[SimpleController]
  }
}

object ServerMain extends Server
