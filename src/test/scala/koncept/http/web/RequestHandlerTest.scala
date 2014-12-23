package koncept.http.web

import org.scalatest.FlatSpec

import koncept.http.io.RequestHandlerUtils
import koncept.http.web.context.RequestContext
import koncept.http.web.requestfilter.UrlFilter
import koncept.http.web.response.WebResponse

class RequestHandlerTest extends FlatSpec with RequestHandlerUtils {

  "A RequestHandler" should "be created with empty handlers" in {
    assert(new RH().handlerChains.isEmpty)
  }
  
  it should "be able to create multiple handler chains" in {
    val rh = new RH
    assert(null != rh.handle())
    assert(null != rh.handle())
  }
  
  "A Single Handler chain" should " be able to create multiple nested handler chains" in {
    val chain = new RH().handle()
    assert(null != chain.apply())
    assert(null != chain.apply())
    assert(null != chain.apply().apply())
  }
  
  "A Handler Chain" should "handle nested request handlers" in {
    val inner = new RH();
    inner.handle(UrlFilter("/inner"))  ((request: RequestContext[Any]) => {
      new WebResponse(500)
    })
    
    val outer = new RH();
    outer.handle(UrlFilter("/outer")).apply(inner)
    
    assert(acceptsHandler("/outer/inner", outer))
    
    assert(! acceptsHandler("/inner/outer", outer))
    
    assert(! acceptsHandler("/outer", outer))
    assert(! acceptsHandler("/inner", outer))
    
    assert(! acceptsHandler("/outer/not", outer))
    assert(! acceptsHandler("/not/inner", outer))
  }
  
  class RH extends RequestHandler[Any] {}
}