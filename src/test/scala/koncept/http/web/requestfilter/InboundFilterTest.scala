package koncept.http.web.requestfilter

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URI
import scala.collection.JavaConversions.mapAsJavaMap
import org.scalatest.FlatSpec
import koncept.http.server.ConfigurationOption
import koncept.http.server.exchange.HttpExchangeImpl
import koncept.http.web.context.RequestContext
import koncept.io.StreamingSocketConnection
import koncept.http.web.RequestHandler
import koncept.http.web.response.WebResponse

class InboundFilterTest extends FlatSpec {

  "A UrlFilter(/)" should "match the root url" in {
    assert(accepts("/", UrlFilter("/")))
  }

  it should "not match any other content" in {
    assert(!accepts("/content", UrlFilter("/")))
    assert(!accepts("/content2/", UrlFilter("/")))
    assert(!accepts("/deep/content", UrlFilter("/")))
  }

  "A UrlFilter(/*)" should "match the root url" in {
    assert(accepts("/", UrlFilter("/*")))
  }

  it should "match any single depth urls content" in {
    assert(accepts("/content", UrlFilter("/*")))
    assert(accepts("/more_content", UrlFilter("/*")))
  }

  it should "not match any deep urls content" in {
    assert(!accepts("/deep/content", UrlFilter("/*")))
    assert(!accepts("/more_stuff/", UrlFilter("/*")))
  }

  "A UrlFilter(/**)" should "match the root url" in {
    assert(accepts("/", UrlFilter("/**")))
  }

  it should "match any single depth urls content" in {
    assert(accepts("/content", UrlFilter("/**")))
    assert(accepts("/more_content", UrlFilter("/**")))
  }

  it should "match any deep urls content" in {
    assert(accepts("/deep/content", UrlFilter("/**")))
    assert(accepts("/more_content/", UrlFilter("/**")))
  }

  "A UrlFilter(/static)" should "match the root url" in {
    assert(accepts("/static", UrlFilter("/static")))
  }

  it should "not match any nested content" in {
    assert(! accepts("/static", UrlFilter("/static/")))
    assert(! accepts("/static", UrlFilter("/static/other")))
    assert(! accepts("/static", UrlFilter("/static ")))
    assert(! accepts("/static", UrlFilter("/staticother")))
  }

  "A UrlFilter(/@varName)" should " match the root url" in {
    val r = result("/", UrlFilter("/@varName"))
    assert(accepts(r))
    assert(r.get.parameters.containsKey("varName"))
    assert(r.get.parameters("varName") == "")
  }

  "A UrlFilter(/@varName)" should " match and bind a simple url" in {
    val r = result("/test", UrlFilter("/@varName"))
    assert(accepts(r))
    assert(r.get.parameters.containsKey("varName"))
    assert(r.get.parameters("varName") == "test")
  }

  def accepts(url: String, filter: UrlFilter[Any]): Boolean = {
    accepts(result(url, filter))
  }
  def accepts(result: Option[RequestContext[Any]]): Boolean = {
    result.isDefined
  }
  def result(url: String, filter: UrlFilter[Any]): Option[RequestContext[Any]] = {
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

    var rh = new RequestHandler[Any] {
      handle(filter)((request: RequestContext[Any]) => {
        new WebResponse(500)
      })
    }

    var endpoint = rh.endpoint(rc)
    if (endpoint.isDefined)
      Some(endpoint.get.rc)
    else
      None
  }

}