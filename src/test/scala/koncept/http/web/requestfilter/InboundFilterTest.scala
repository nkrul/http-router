package koncept.http.web.requestfilter

import java.net.URI

import org.scalatest.FlatSpec

import koncept.http.server.exchange.HttpExchangeImpl
import koncept.http.web.RequestContextMutator
import koncept.http.web.context.RequestContext

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
    var exchange = new HttpExchangeImpl(null, null, null, "HTTP/0.9", "GET", new URI(url), null, null) //{
//      override def getRequestURI(): URI = {
//        return new URI(url)
//      }
//    }
    var rc = new RequestContext(exchange, null, null)
    
    filter.accepts(rc, new RequestContextMutator(url))
  }
  
  
  
  
}