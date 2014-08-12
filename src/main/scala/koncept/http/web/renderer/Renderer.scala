package koncept.http.web.renderer

import java.io.IOException
import com.sun.net.httpserver.HttpExchange
import java.util.Map
import java.io.OutputStream

trait Renderer {

  @throws(classOf[IOException])
  def render(
      responseCode: Int, 
      template: String, 
      exchange: HttpExchange, 
      rootMap: Map[String, Any], //java map... for better or worse
      mimeType: String = "text/html"
      ) : Unit;
  
  @throws(classOf[IOException])
  def render(
      template: String, 
      out: OutputStream,
      rootMap: Map[String, Any] //java map... for better or worse
      ) : Unit;
}