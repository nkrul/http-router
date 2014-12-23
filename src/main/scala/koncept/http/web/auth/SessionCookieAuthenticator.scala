package koncept.http.web.auth

import com.sun.net.httpserver.Authenticator
import com.sun.net.httpserver.HttpExchange
import koncept.http.web.sessions.Sessions
import koncept.http.web.sessions.Session
import koncept.http.web.cookie.CookieHelper
import com.sun.net.httpserver.HttpContext



class SessionCookieAuthenticator(val sessionCookieName: String = "sessionId", var minutesTimeout: Int = 20) extends Authenticator {
  

  def login(username: String, realm: String)(implicit exchange: HttpExchange): Session = {
    ensureNoExistingSession
    val principal = new RichPrincipal(username, realm)
    val session = Sessions(exchange.getHttpContext().getAttributes()).create(principal)
    CookieHelper(exchange).create(sessionCookieName, session.id)
    exchange.setAttribute("session", session)
    session
  }
  
  def logout(implicit exchange: HttpExchange) {
    ensureNoExistingSession
    CookieHelper(exchange).delete(sessionCookieName)
    exchange.setAttribute("session", null)
  }
  
  def cleanup(httpContext: HttpContext) {
    Sessions(httpContext.getAttributes()).cleanup(minutesTimeout)
  }
  
  private def ensureNoExistingSession(implicit exchange: HttpExchange) {
    exchange.getAttribute("session") match {
      case null =>{}
      case session: Session => {
        Sessions(exchange.getHttpContext().getAttributes()).destroy(session.id)
        }
    }
  }
  
  override def authenticate (exchange: HttpExchange): Authenticator.Result = {
    val sessionCookie = CookieHelper(exchange).getCookie(sessionCookieName)
    if (sessionCookie.isEmpty)
      return new Authenticator.Success(null) //work around - no principal = not authenticated
    val session = Sessions(exchange.getHttpContext().getAttributes()).get(sessionCookie.get)
    if (session == null) 
      return new Authenticator.Success(null)
    
    //consider adding in extra 'IP' or 'UserAgent' code
//    exchange.getRemoteAddress().getAddress().getAddress() //byte array
    
    session.principal.asInstanceOf[RichPrincipal].touch
    exchange.setAttribute("session", session)
    return new Authenticator.Success(session.principal)
  }
}

class SessionCookieAuthenticatorCleaner(authenticator: SessionCookieAuthenticator, httpContext: HttpContext) extends Runnable {
  override def run() {
    authenticator.cleanup(httpContext)
  }
}