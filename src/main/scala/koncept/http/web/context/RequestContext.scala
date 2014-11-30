package koncept.http.web.context

import com.sun.net.httpserver.HttpExchange

import koncept.http.web.auth.SessionCookieAuthenticator
import koncept.http.web.renderer.RendererFactory
import koncept.http.web.sessions.Session

case class RequestContext[R] (
    exchange: HttpExchange,
    resources: R,
    rendererFactory: RendererFactory,
    url: String,
    parameters: Map[String, Any]
    ) {
  def this(exchange: HttpExchange, resources: R, rendererFactory: RendererFactory) =
    this(
        exchange,
        resources,
        rendererFactory,
        if (exchange.getRequestURI.toString.indexOf("?") == -1) //url
      	  exchange.getRequestURI.toString
      	else
      	  exchange.getRequestURI.toString.substring(0, exchange.getRequestURI.toString.indexOf("?")),
        Map[String, Any]()
        )
   def this(rc: RequestContext[R], kv: Tuple2[String, Any]*) = this(
       rc.exchange,
       rc.resources,
       rc.rendererFactory,
       rc.url,
       rc.parameters ++ kv
       )
       
  //TODO change this to a Map[String,String]
  var urlParams = 
    if (exchange.getRequestURI.toString.indexOf("?") == -1)
	  ""
	else
	  exchange.getRequestURI.toString.substring(exchange.getRequestURI.toString.indexOf("?") + 1)
  
  def httpSession: Option[Session] = {
    val session = exchange.getAttribute("session")
    if (session != null) session match {
      case s: Session => return Some(s)
    }
    return None
  }
  
  def login(username: String, realm: String = ""): Session = {
    val httpAuthenticator = exchange.getHttpContext().getAuthenticator()
    httpAuthenticator match {
      case authenticator: SessionCookieAuthenticator => {
        authenticator.login(username, realm)(exchange)
      }
    }
  }
  
  def logout() {
    val httpAuthenticator = exchange.getHttpContext().getAuthenticator()
    httpAuthenticator match {
      case authenticator: SessionCookieAuthenticator => {
        authenticator.logout(exchange)
      }
    }
  }
}