package koncept.http.web

import koncept.http.web.context.RequestContext
import koncept.http.web.requestfilter.InboundFilter
import koncept.http.web.response.WebResponse

trait RequestHandler[R] {
  var handlers: List[EndpointHandler[R]] = Nil
  var filters: List[FilterHandler[R]] = Nil

  def endpoint(rc: RequestContext[R]): Option[EndpointEvent[R]] = {
    var response: Option[EndpointEvent[R]] = None
    for (handler <- handlers) yield {
      if (response.isEmpty) {
        response = handler.applies(rc)
      }
    }
    response
  }
  
  def listeners(rc: RequestContext[R]): List[FilterEvent[R]] = {
    var listeners: List[FilterEvent[R]] = Nil
    for (filter <- filters) {
      var response = filter.listens(rc)
      if (response.isDefined) {
        listeners ::= response.get
      }
    }
    listeners
  }

  def handle(filters: InboundFilter[R]*): HandlerChain[R] = {
    new HandlerChain[R](this, null, filters)
  }
}

class HandlerChain[R](handler: RequestHandler[R], val parent: HandlerChain[R], val filters: Seq[InboundFilter[R]]) {

  def apply(op: RequestContext[R] => WebResponse): HandlerChain[R] = {
    handler.handlers ::= new EndpointHandler[R](filters, op)
    this
  }

  def apply(filters: InboundFilter[R]*): HandlerChain[R] = new HandlerChain[R](handler, this, filters)

  def apply(filter: () => RequestEventListener[R]): HandlerChain[R] = {
    handler.filters ::= new FilterHandler[R](filters, filter)
    this
  }
  
}

class EndpointHandler[R](filters: Seq[InboundFilter[R]], op: RequestContext[R] => WebResponse) {
  def applies(rc: RequestContext[R]): Option[EndpointEvent[R]] = {
    for(filter <- filters) {
        val filterResponse = filter.accepts(rc, Map[String, Any]())
        if (filterResponse.acceptable)
          return Some(new EndpointEvent(filterResponse.rc, op))
    }
    None
  }
}

class EndpointEvent[R](val rc: RequestContext[R], op: RequestContext[R] => WebResponse) {
  def respond(rc: RequestContext[R]): WebResponse = {
    op.apply(rc)
  }
}

class FilterHandler[R](filters: Seq[InboundFilter[R]], filter: () => RequestEventListener[R]) {
  def listens(rc: RequestContext[R]): Option[FilterEvent[R]] = {
    for(requestFilter <- filters) {
      val filterResponse = requestFilter.accepts(rc, Map[String, Any]())
      if (filterResponse.acceptable)
        return Some(new FilterEvent(filter))
    } 
    None
  }
}

class FilterEvent[R](filter: () => RequestEventListener[R]) {
  def apply(): RequestEventListener[R] = {
    filter()
  }
}







