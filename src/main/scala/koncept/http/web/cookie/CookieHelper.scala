package koncept.http.web.cookie

import scala.collection.JavaConversions.asScalaBuffer

import com.sun.net.httpserver.HttpExchange

class CookieHelper(exchange: HttpExchange) {

  def getCookie(name: String): Option[String] = {
    var matchedCookie: Option[String] = None
    val headers = exchange.getRequestHeaders().get("Cookie")
    
    if (headers != null) {
      for (header <- headers) {
    	  header.split(";").foreach(rawCookie => {
    		  val cookie = rawCookie.trim()
    		  if (matchedCookie.isEmpty && cookie.startsWith(name + "="))
    			  matchedCookie = Some(cookie.substring(name.length() + 1))
    	  })
      }
    }
    return matchedCookie
  }

  def cookie(name:String, value: String) : String = {
    name + "=" + value + "; Path=/" 
  }
  
  
  
  
  
  /*
cookie strings (from wikipedia)
Set-Cookie: name2=value2; Expires=Wed, 09 Jun 2021 10:18:14 GMT
Set-Cookie: HSID=AYQEVn….DKrdst; Domain=.foo.com; Path=/; Expires=Wed, 13 Jan 2021 22:23:01 GMT; HttpOnly
Set-Cookie: reg_fb_gate=deleted; Expires=Thu, 01 Jan 1970 00:00:01 GMT; Path=/; Domain=.example.com; HttpOnly

   */
  
  //TODO: proper cookie handling... INCLUDING expiry
  def create(name: String, value: String) = {
    //println("setting cookie to " + cookie(name, value))
    exchange.getResponseHeaders().add("Set-Cookie", cookie(name, value))
  }
  
  def delete(name: String) = {
    exchange.getResponseHeaders().add("Set-Cookie", name + "=_; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT")
  }

}
object CookieHelper {
  def apply(exchange: HttpExchange) = new CookieHelper(exchange)
}