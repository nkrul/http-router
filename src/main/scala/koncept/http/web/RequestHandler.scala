package koncept.http.web

import koncept.http.web.context.RequestContext
import koncept.http.web.requestfilter.InboundFilter
import koncept.http.web.response.WebResponse

trait RequestHandler[R] { //extends RequestChainActionable[R] {
  var handlers: List[EndpointHandler[R]] = Nil
  var filters: List[FilterHandler[R]] = Nil

  def applies(rc: RequestContext[R], onAccept: RequestContextMutator): Option[EndpointEvent[R]] = {
    var response: Option[EndpointEvent[R]] = None
    for (handler <- handlers) yield {
      if (response.isEmpty) {
        response = handler.applies(rc, onAccept.clone)
      }
    }
    response
  }
  
  def listeners(rc: RequestContext[R], onAccept: RequestContextMutator): List[FilterEvent[R]] = {
    var listeners: List[FilterEvent[R]] = Nil
    for (filter <- filters) {
      var response = filter.listens(rc, onAccept.clone)
      if (response.isDefined) {
        listeners ::= response.get
      }
    }
    listeners
  }

  def handle(filters: InboundFilter*): HandlerChain[R] = {
    new HandlerChain[R](this, null, filters)
  }
}

class RequestContextMutator(var url: String, var parameters: Map[String, List[String]] = Map()) {
  def addParameter(key: String, value: String) {
    var list = parameters.getOrElse(key, Nil)
    list ::= value
    parameters += (key -> list)
  }

  def mutate(rc: RequestContext[_]) {
    rc.mergeParams(parameters)
  }

  override def clone: RequestContextMutator = {
    var params: Map[String, List[String]] = Map();
    params ++= parameters //*want* to do a parameters.clone
    val that = new RequestContextMutator(url, params)
    that
  }

}

class HandlerChain[R](handler: RequestHandler[R], val parent: HandlerChain[R], val filters: Seq[InboundFilter]) {

  def apply(op: RequestContext[R] => WebResponse): HandlerChain[R] = {
    handler.handlers ::= new EndpointHandler[R](merge(), op)
    this
  }

  def apply(filters: InboundFilter*): HandlerChain[R] = new HandlerChain[R](handler, this, filters)

  def apply(filter: () => RequestEventListener[R]): HandlerChain[R] = {
    handler.filters ::= new FilterHandler[R](merge(), filter)
    this
  }

  //mmm... this kinda shuffles the list
  private def merge(): List[InboundFilter] = {
    var toMerge: List[InboundFilter] = filters.toList
    var current = parent
    while (current != null) {
      toMerge ++ current.filters
      current = current.parent
    }
    toMerge = toMerge.reverse

    var merged: List[InboundFilter] = Nil
    for (childFilter <- toMerge) {
      val mergedFilter = merged.filter((p: InboundFilter) => { p.getClass == childFilter.getClass }).foldLeft(childFilter)((left: InboundFilter, right: InboundFilter) => { left.merge(right) })
      merged = merged.filterNot((p: InboundFilter) => { p.getClass == childFilter.getClass })
      merged ::= mergedFilter
    }
    merged
  }

}

class EndpointHandler[R](filters: List[InboundFilter], op: RequestContext[R] => WebResponse) {
  def applies(rc: RequestContext[R], onAccept: RequestContextMutator): Option[EndpointEvent[R]] = {
    if (isDenied(rc, onAccept))
      return None
    Some(new EndpointEvent(op, onAccept))
  }
  private def isDenied(rc: RequestContext[R], onAccept: RequestContextMutator): Boolean = {
    var allowed = true
    for (filter <- filters) {
      if (allowed && !filter.accepts(rc, onAccept))
        allowed = false
    }
    !allowed
  }
}

class EndpointEvent[R](op: RequestContext[R] => WebResponse, onAccept: RequestContextMutator) {
  def prepare(rc: RequestContext[R]) {
    onAccept.mutate(rc)
  }
  def respond(rc: RequestContext[R]): WebResponse = {
    op.apply(rc)
  }
}

class FilterHandler[R](filters: List[InboundFilter], filter: () => RequestEventListener[R]) {
  def listens(rc: RequestContext[R], onAccept: RequestContextMutator): Option[FilterEvent[R]] = {
    if (isDenied(rc, onAccept))
      return None
    Some(new FilterEvent(filter))
  }
  private def isDenied(rc: RequestContext[R], onAccept: RequestContextMutator): Boolean = {
    var allowed = true
    for (filter <- filters) {
      if (allowed && !filter.accepts(rc, onAccept))
        allowed = false
    }
    !allowed
  }
}

class FilterEvent[R](filter: () => RequestEventListener[R]) {
  def apply(): RequestEventListener[R] = {
    filter()
  }
}







