package koncept.http.web.delegate

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler

class DispatcherDelegate(var wrapped: HttpHandler = null) extends HttpHandler {
  def handle(exchange: HttpExchange) {
    try {
      wrapped.handle(exchange)
    } catch {
      case t: Throwable => t.printStackTrace()
    }
  }
}
object DispatcherDelegate {
  def apply(wrapped: HttpHandler): DispatcherDelegate = new DispatcherDelegate(wrapped)
}