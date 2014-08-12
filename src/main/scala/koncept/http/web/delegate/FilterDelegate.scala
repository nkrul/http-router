package koncept.http.web.delegate

import com.sun.net.httpserver.Filter
import com.sun.net.httpserver.HttpExchange


class FilterDelegate(var wrapped: Filter = null) extends Filter {
  def doFilter(exchange: HttpExchange, chain: Filter.Chain) = {
    try {
      wrapped.doFilter(exchange, chain)
    } catch {
      case t: Throwable => t.printStackTrace()
    }
  }
  def description(): String = {
    "wrapped: " + wrapped.description()
  }
}
object FilterDelegate {
  def apply(wrapped: Filter): FilterDelegate = new FilterDelegate(wrapped)
}