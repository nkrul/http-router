package koncept.http.web.requestfilter

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.URI

import scala.collection.JavaConversions.mapAsJavaMap

import org.scalatest.FlatSpec

import koncept.http.server.ConfigurationOption
import koncept.http.server.exchange.HttpExchangeImpl
import koncept.http.web.RequestContextMutator
import koncept.http.web.context.RequestContext
import koncept.io.StreamingSocketConnection

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

  
  def accepts(url: String, filter: UrlFilter): Boolean = {
    val exchangeOptions: Map[ConfigurationOption, String] = Map((HttpExchangeImpl.ATTRIBUTE_SCOPE -> "exchange"))
    val ssc = new StreamingSocketConnection[Any]() {
	    def localAddress() : InetSocketAddress = null
	    def remoteAddress() : InetSocketAddress = null
	    def in() : InputStream = null
	    def out() : OutputStream = null
	    def setWriteTimeout(writeTimeout: Long) {}
	    def setReadTimeout(readTimeout: Long) {}
	    def close() {}
    }
    var exchange = new HttpExchangeImpl(ssc, "HTTP/0.9", "GET", new URI(url), null, exchangeOptions)
    var rc = new RequestContext(exchange, null, null)
    
    filter.accepts(rc, new RequestContextMutator(url))
  }
  
  
  
  
}