package koncept.http.web.auth

import java.util.Date

import com.sun.net.httpserver.HttpPrincipal

class RichPrincipal(username: String, realm: String) extends HttpPrincipal(username, realm) {
  
  private var lastTouchTS = System.currentTimeMillis()
  var data: Map[Any, Any] = Map()
  
  def lastTouch : Date = {
    new Date(lastTouchTS)
  }
  def touch() = {
    lastTouchTS = System.currentTimeMillis()
  }
  
}