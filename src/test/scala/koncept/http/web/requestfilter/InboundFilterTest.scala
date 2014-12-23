package koncept.http.web.requestfilter

import scala.collection.JavaConversions.mapAsJavaMap

import org.scalatest.FlatSpec

import koncept.http.io.RequestHandlerUtils
import koncept.http.web.RequestHandler
import koncept.http.web.context.RequestContext
import koncept.http.web.response.WebResponse

class InboundFilterTest extends FlatSpec with RequestHandlerUtils {

  "A UrlFilter(/)" should "match the root url" in {
    assert(acceptsFilter("/", UrlFilter("/")))
  }

  it should "not match any other content" in {
    assert(! acceptsFilter("/content", UrlFilter("/")))
    assert(! acceptsFilter("/content2/", UrlFilter("/")))
    assert(! acceptsFilter("/deep/content", UrlFilter("/")))
  }

  "A UrlFilter(/*)" should "match the root url" in {
    assert(acceptsFilter("/", UrlFilter("/*")))
  }

  it should "match any single depth urls content" in {
    assert(acceptsFilter("/content", UrlFilter("/*")))
    assert(acceptsFilter("/more_content", UrlFilter("/*")))
  }

  it should "not match any deep urls content" in {
    assert(! acceptsFilter("/deep/content", UrlFilter("/*")))
    assert(! acceptsFilter("/more_stuff/", UrlFilter("/*")))
  }

  "A UrlFilter(/**)" should "match the root url" in {
    assert(acceptsFilter("/", UrlFilter("/**")))
  }

  it should "match any single depth urls content" in {
    assert(acceptsFilter("/content", UrlFilter("/**")))
    assert(acceptsFilter("/more_content", UrlFilter("/**")))
  }

  it should "match any deep urls content" in {
    assert(acceptsFilter("/deep/content", UrlFilter("/**")))
    assert(acceptsFilter("/more_content/", UrlFilter("/**")))
  }

  "A UrlFilter(/static)" should "not match the url prefix" in {
    assert(! acceptsFilter("/", UrlFilter("/static")))
  }
  
  it should "match the url prefix" in {
    assert(acceptsFilter("/static", UrlFilter("/static")))
  }

  it should "not match a different url prefix" in {
    assert(! acceptsFilter("/other", UrlFilter("/static")))
  }
  
  it should "not match any nested content" in {
    assert(! acceptsFilter("/static", UrlFilter("/static/")))
    assert(! acceptsFilter("/static", UrlFilter("/static/other")))
    assert(! acceptsFilter("/static", UrlFilter("/static ")))
    assert(! acceptsFilter("/static", UrlFilter("/staticother")))
  }
  

  "A UrlFilter(/@varName)" should "not  match the root url" in {
    var rh = new RequestHandler[Any] {
      handle(UrlFilter("/@varName"))  ((request: RequestContext[Any]) => {
        new WebResponse(500)
      })
    }
    //@varName should NOT bind the empty string
    assert(! accepts(result("/", rh)))
  }

  it should " match and bind a simple url" in {
    var rh = new RequestHandler[Any] {
      handle(UrlFilter("/@varName"))  ((request: RequestContext[Any]) => {
        new WebResponse(500)
      })
    }
    
    val r = result("/test", rh)
    assert(accepts(r))
    assert(r.get.parameters.containsKey("varName"))
    assert(r.get.parameters("varName") == "test")
  }
}