package koncept.http.web.sessions

import com.sun.net.httpserver.HttpPrincipal

class Session(val principal: HttpPrincipal, val id: String) {
  private var data: Map[Any, Any] = Map()
  private var lastTouchTime = System.currentTimeMillis()
  
  def set(key: Any, value: Any) {
    data = data + (key -> value)
  }
  def apply(key: Any, value: Any) {
    data = data + (key -> value)
  }
  
  def get(key: Any) {
    data.get(key)
  }
  def apply(key: Any) : Any = {
    data(key)
  }
  
  def touch() {
    lastTouchTime = System.currentTimeMillis()
  }
  
  def lastTouch: Long = {
    lastTouchTime
  }
  
}