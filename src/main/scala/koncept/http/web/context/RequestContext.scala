package koncept.http.web.context

import com.sun.net.httpserver.HttpExchange

import koncept.http.web.auth.SessionCookieAuthenticator
import koncept.http.web.renderer.RendererFactory
import koncept.http.web.sessions.Session

class RequestContext[R](val exchange: HttpExchange, requestResources: R, val rendererFactory: RendererFactory) {
  val url = 
    if (exchange.getRequestURI.toString.indexOf("?") == -1)
	  exchange.getRequestURI.toString
	else
	  exchange.getRequestURI.toString.substring(0, exchange.getRequestURI.toString.indexOf("?"))
  var urlParams = 
    if (exchange.getRequestURI.toString.indexOf("?") == -1)
	  ""
	else
	  exchange.getRequestURI.toString.substring(exchange.getRequestURI.toString.indexOf("?") + 1)
  
	  
  def resources: R = {
    requestResources
  }
  
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

  def parameters: Map[String, Any] = {
    exchange.getAttribute("parameters").asInstanceOf[Map[String, Any]]
  }
  def params: Map[String, String] = {
    var attr = exchange.getAttribute("params")
    if (attr != null)
      attr.asInstanceOf[Map[String, String]]
    else {
      buildSimpleParams
    }
  }

  def mergeParams(params: Map[String, Any]) {
    var exchangeParams = parameters
    for(key <- params.keys) {
      exchangeParams += (key -> params(key))
    }
    exchange.setAttribute("parameters", exchangeParams)
    buildSimpleParams
  }
  def buildSimpleParams(): Map[String, String] = {
    var params: Map[String, String] = Map()
    for (key <- parameters.keys) {
      val value = parameters.get(key).get;
      value match {
        case s: String => params += (key -> s)
        case l: List[String] => params += key -> l(0)
        case _ => {}
      }
    }
    exchange.setAttribute("params", params) //shorthand params support
    params
  }
  
//  def parameterFilter: ParameterFilter = {
//    exchange.getAttribute("parameterFilter").asInstanceOf[ParameterFilter]
//  }

}