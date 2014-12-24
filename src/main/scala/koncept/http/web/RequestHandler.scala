package koncept.http.web

import koncept.http.web.context.RequestContext
import koncept.http.web.requestfilter.AcceptsResponse
import koncept.http.web.requestfilter.FilterResponse
import koncept.http.web.requestfilter.InboundFilter
import koncept.http.web.response.WebResponse

trait RequestHandler[R] extends EndpointFinder[R] {
  var handlerChains: List[HandlerChain[R]] = Nil

  def endpointEvent(rc: RequestContext[R]): Option[EndpointEvent[R]] = endpointEvent(rc, Map[String, Any]())

  def endpointEvent(rc: RequestContext[R], cache: Map[String, Any]): Option[EndpointEvent[R]] = {
    var result: Option[EndpointEvent[R]] = None
    for (handler <- handlerChains) {
      result = handler.endpointEvent(rc, cache)
      if (result.isDefined)
        return result
    }
    None
  }

  def filterEvents(rc: RequestContext[R]): List[FilterEvent[R]] = filterEvents(rc, Map[String, Any]())

  def filterEvents(rc: RequestContext[R], cache: Map[String, Any]): List[FilterEvent[R]] = {
    var result: List[FilterEvent[R]] = Nil
    for (handler <- handlerChains) {
      result ++= handler.filterEvents(rc, cache)
    }
    result
  }

  def handle(filters: InboundFilter[R]*): HandlerChain[R] = {
    val hc = new HandlerChain[R](filters)
    handlerChains ::= hc
    hc
  }

  def apply(filters: InboundFilter[R]*): HandlerChain[R] = {
    val hc = new HandlerChain[R](filters)
    handlerChains ::= hc
    hc
  }
}

class HandlerChain[R](filters: Seq[InboundFilter[R]]) extends EndpointFinder[R] {
  private var endpointHandlers: List[EndpointFinder[R]] = Nil
  private var handlerChains: List[EndpointFinder[R]] = Nil

  def endpointEvent(rc: RequestContext[R], cache: Map[String, Any]): Option[EndpointEvent[R]] = {
    var result: Option[EndpointEvent[R]] = None
    var filterResponse: FilterResponse[R] = AcceptsResponse(rc, cache);
    
    //1: ensure that this handler chain applies
    for (filter <- filters)
      if (filterResponse.continuable)
        filterResponse = filter.accepts(rc, filterResponse.filterArgs)

    //2: On exact match, take any terminal states
    if (filterResponse.acceptable)
      for (handler <- endpointHandlers)
        if (!result.isDefined)
          result = handler.endpointEvent(filterResponse.rc, filterResponse.filterArgs)

    //3: If no exact match, continue to parse for continuable matches
    if (filterResponse.continuable && !result.isDefined)
      for (handler <- handlerChains)
        if (!result.isDefined)
          result = handler.endpointEvent(filterResponse.rc, filterResponse.filterArgs)

    result
  }

  def filterEvents(rc: RequestContext[R], cache: Map[String, Any]): List[FilterEvent[R]] = {
    var result: List[FilterEvent[R]] = Nil
    var filterResponse: FilterResponse[R] = AcceptsResponse(rc, cache);
    
    //1: ensure that this handler chain applies
    for (filter <- filters)
      if (filterResponse.continuable)
        filterResponse = filter.accepts(rc, filterResponse.filterArgs)  

    //2: add any applicable filter events to the list
    if (filterResponse.acceptable || filterResponse.continuable) {
      for (handler <- endpointHandlers)
        result ++= handler.filterEvents(filterResponse.rc, filterResponse.filterArgs)
      for (handler <- handlerChains)
        result ++= handler.filterEvents(filterResponse.rc, filterResponse.filterArgs)
    }
    result
  }

  def apply(filters: InboundFilter[R]*): HandlerChain[R] = {
    val hc = new HandlerChain[R](filters)
    endpointHandlers ::= hc
    hc
  }

  def apply(op: RequestContext[R] => WebResponse) {
    endpointHandlers ::= new EndpointHandler[R](op)
  }

  def apply(filter: () => RequestEventListener[R]) {
    endpointHandlers ::= new FilterHandler[R](filter)
  }

  def apply(rh: RequestHandler[R]) {
    handlerChains ::= rh
  }
}

trait EndpointFinder[R] {
  def endpointEvent(rc: RequestContext[R], cache: Map[String, Any]): Option[EndpointEvent[R]]
  def filterEvents(rc: RequestContext[R], cache: Map[String, Any]): List[FilterEvent[R]]
}

class EndpointHandler[R](op: RequestContext[R] => WebResponse) extends EndpointFinder[R] {
  def endpointEvent(rc: RequestContext[R], cache: Map[String, Any]): Option[EndpointEvent[R]] =
    Some(new EndpointEvent(rc, op))
  def filterEvents(rc: RequestContext[R], cache: Map[String, Any]): List[FilterEvent[R]] =
    Nil
}

class EndpointEvent[R](val rc: RequestContext[R], op: RequestContext[R] => WebResponse) {
  def respond(rc: RequestContext[R]): WebResponse = {
    op.apply(rc)
  }
}

class FilterHandler[R](filter: () => RequestEventListener[R]) extends EndpointFinder[R] {
  def endpointEvent(rc: RequestContext[R], cache: Map[String, Any]): Option[EndpointEvent[R]] = None
  def filterEvents(rc: RequestContext[R], cache: Map[String, Any]): List[FilterEvent[R]] = List(new FilterEvent(filter))
}

class FilterEvent[R](filter: () => RequestEventListener[R]) {
  def apply(): RequestEventListener[R] = {
    filter()
  }
}