package koncept.http.web

import org.scalatest.FlatSpec

class RequestHandlerTest extends FlatSpec {

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
  
  class RH extends RequestHandler[Any] {}
}