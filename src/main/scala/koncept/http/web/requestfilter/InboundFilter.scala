package koncept.http.web.requestfilter

import koncept.http.web.context.RequestContext

trait InboundFilter[R] {
  def accepts(rc: RequestContext[R], cache: Map[String, Any]): FilterResponse[R]
}

abstract class FilterResponse[R] {
  def acceptable: Boolean
  def continuable: Boolean
  def rc: RequestContext[R]
  def filterArgs: Map[String, Any]
}

case class AcceptsResponse[R](newContext: RequestContext[R], cache: Map[String, Any]) extends FilterResponse[R] {
  override def acceptable = {
    val remaining = cache.get("remainingUrl")
    if(remaining.isDefined)
      remaining.get.toString() equals ""
    else
      true
  }
  override def continuable = true
  override def rc = newContext
  override def filterArgs = cache
}

case class RejectsResponse[R]() extends FilterResponse[R] {
  override def acceptable: Boolean = false
  override def continuable = false
  override def rc = null
  override def filterArgs = null
}


case class UrlFilter [R] (url: String) extends InboundFilter[R] with UrlParsingFunctions[R] {
  var fragments: List[InboundFilter[R]] = Nil
  var fragment = head(url)
  var remaining = tail(url)
  while (!fragment.equals("")) {
    if (fragment.equals("**")) {
      fragments ::= new AnyUrl()
    } else if (fragment.equals("*")) {
      fragments ::= new AnyUrlFragment()
    } else if (fragment.startsWith("@")) {
      fragments ::= new UrlFragmentAsProperty(fragment.substring(1))
    } else {
      fragments ::= new StaticUrlFragmentFilter(fragment)
    }
    fragment = head(remaining)
    remaining = tail(remaining)
  }
  fragments = fragments.reverse

  def accepts(rc: RequestContext[R], cache: Map[String, Any]): FilterResponse[R] = {
    var result : FilterResponse[R] = AcceptsResponse(rc, cache);
    for (fragmentFilter <- fragments)
      if (result.continuable)
        result = fragmentFilter.accepts(result.rc, result.filterArgs)
    result
  }
}

trait UrlParsingFunctions[R] {
  def head(url: String): String = {
    if (url.startsWith("/"))
      return "/"
    var index = url.indexOf("/")
    if (index != -1)
      return url.substring(0, index)
    url
  }
  def tail(url: String): String = {
    if (url.startsWith("/"))
      return url.substring(1)
    var index = url.indexOf("/")
    if (index != -1)
      return url.substring(index)
    ""
  }
  
  def url(cache: Map[String, Any], url: String) : Map[String, Any] = cache + ("remainingUrl" -> url)
  
  def url(rc: RequestContext[R], cache: Map[String, Any]) : String = {
    val remaining = cache.get("remainingUrl")
    if(remaining.isDefined)
      remaining.get.toString()
    else
      rc.url
  }
}

class StaticUrlFragmentFilter[R](fragment: String) extends InboundFilter[R] with UrlParsingFunctions[R] {
  def accepts(rc: RequestContext[R], cache: Map[String, Any]): FilterResponse[R] = {
    if (head(url(rc, cache)).equals(fragment)) {
      val remainingUrl = tail(url(rc, cache))
      AcceptsResponse(
          new RequestContext(rc, ("**" -> remainingUrl)),
          url(cache, remainingUrl))
    } else {
      RejectsResponse[R]
    }
  }
}
class AnyUrl[R]() extends InboundFilter[R] with UrlParsingFunctions[R] {
  def accepts(rc: RequestContext[R], cache: Map[String, Any]): FilterResponse[R] = {
    AcceptsResponse(
        new RequestContext(rc, ("**" -> url(rc, cache))),
        url(cache, ""))
  }
}
class AnyUrlFragment[R]() extends InboundFilter[R] with UrlParsingFunctions[R] {
  def accepts(rc: RequestContext[R], cache: Map[String, Any]): FilterResponse[R] = {
    val remainingUrl = tail(url(rc, cache))
    AcceptsResponse(rc, url(cache, remainingUrl))
  }
}
class UrlFragmentAsProperty[R](propertyName: String) extends InboundFilter[R] with UrlParsingFunctions[R] {
  def accepts(rc: RequestContext[R], cache: Map[String, Any]): FilterResponse[R] = {
    val h = head(url(rc, cache))
    if (h == "")
      return RejectsResponse[R] //don't bind to EMPTY variables 
    val t = tail(url(rc, cache))
    AcceptsResponse(
        new RequestContext(rc, (propertyName -> head(url(rc, cache)))),
        url(cache, tail(url(rc, cache))))
  }
}

case class RequestMethodFilter[R](methods: String*) extends InboundFilter[R] {
  def accepts(rc: RequestContext[R], cache: Map[String, Any]): FilterResponse[R] = {
    for (method <- methods)
      if(rc.exchange.getRequestMethod.equals(method))
        return AcceptsResponse(rc, cache)
    RejectsResponse[R]
  }
}

case class SessionFilter[R](sessionExists: Boolean = true) extends InboundFilter[R] {
  def accepts(rc: RequestContext[R], cache: Map[String, Any]): FilterResponse[R] = {
    if(rc.httpSession.isDefined == sessionExists)
      return AcceptsResponse(rc, cache)
    else
      RejectsResponse[R]
  }
}