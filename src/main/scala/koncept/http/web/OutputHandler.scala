package koncept.http.web

import java.io.IOException

import scala.collection.JavaConversions.mapAsJavaMap

import koncept.http.web.context.RequestContext
import koncept.http.web.response.WebResponse

trait OutputHandler[R] {

  def handle(request: RequestContext[R], response: WebResponse)
  
}

class TemplatedResponse[R] extends OutputHandler[R] { 
  def handle(rc: RequestContext[R], webResponse: WebResponse) {
    var rootMap = scala.collection.mutable.Map[String, Any]()
    rootMap ++= rc.parameters
    if (webResponse.details != null)
      rootMap ++= webResponse.details
    rootMap += ("rc" -> this);
    if (rc.httpSession.isDefined) {
      rootMap += ("session" -> rc.httpSession.get)
    }
    try {
      rc.rendererFactory.renderer.render(webResponse.responseCode, webResponse.responseData, rc.exchange, rootMap)
    } catch {
      case t: Throwable => {
        t.printStackTrace();
//        we don't cache things... so... we can't replace the page with a 500 error.
//        we *could* write in a cached renderer, I guess... but wouldn't want it on by default
      }
    }
  }
}

//e.g. use to override a 404 page
class DefaultTempleteResponse[R](template: String) extends TemplatedResponse[R] {
  override def handle(rc: RequestContext[R], webResponse: WebResponse) {
    super.handle(rc, new WebResponse(webResponse.responseCode, template, webResponse.details));
  }
}

class RedirectResponse[R] extends OutputHandler[R] {
  def handle(rc: RequestContext[R], webResponse: WebResponse) {
    rc.exchange.getResponseHeaders().add("Location", webResponse.responseData)
    rc.exchange.sendResponseHeaders(webResponse.responseCode, 0)
  }
}

class ErrorCodeResponseResponse[R] extends OutputHandler[R] {
  def handle(rc: RequestContext[R], webResponse: WebResponse) {
    rc.exchange.sendResponseHeaders(webResponse.responseCode, 0)
  }
}

class NoResponse[R] extends OutputHandler[R] {
  def handle(rc: RequestContext[R], webResponse: WebResponse) {
  }
}