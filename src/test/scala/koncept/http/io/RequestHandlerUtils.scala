package koncept.http.io

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URI

import scala.collection.JavaConversions.mapAsJavaMap

import koncept.http.server.ConfigurationOption
import koncept.http.server.exchange.HttpExchangeImpl
import koncept.http.web.RequestHandler
import koncept.http.web.context.RequestContext
import koncept.http.web.requestfilter.UrlFilter
import koncept.http.web.response.WebResponse
import koncept.io.StreamingSocketConnection

trait RequestHandlerUtils {

  def acceptsFilter(url: String, filter: UrlFilter[Any]): Boolean = {
    var rh = new RequestHandler[Any] {
      handle(filter)  ((request: RequestContext[Any]) => {
        new WebResponse(500)
      })
    }
    acceptsHandler(url, rh)
  }
  def acceptsHandler(url: String, rh: RequestHandler[Any]): Boolean = {
    accepts(result(url, rh))
  }
  def accepts(result: Option[RequestContext[Any]]): Boolean = {
    result.isDefined
  }
  
  
  def result(url: String, rh: RequestHandler[Any]): Option[RequestContext[Any]] = {
    val exchangeOptions: Map[ConfigurationOption, String] = Map((HttpExchangeImpl.ATTRIBUTE_SCOPE -> "exchange"))
    val ssc = new StreamingSocketConnection[Any]() {
      def localAddress(): InetSocketAddress = null
      def remoteAddress(): InetSocketAddress = null
      def in(): InputStream = null
      def out(): OutputStream = null
      def setWriteTimeout(writeTimeout: Long) {}
      def setReadTimeout(readTimeout: Long) {}
      def close() {}
    }
    var exchange = new HttpExchangeImpl(ssc, "HTTP/0.9", "GET", new URI(url), null, exchangeOptions)
    var rc = new RequestContext[Any](exchange, null, null)

    var endpoint = rh.endpointEvent(rc)
    if (endpoint.isDefined)
      Some(endpoint.get.rc)
    else
      None
  }
  
}