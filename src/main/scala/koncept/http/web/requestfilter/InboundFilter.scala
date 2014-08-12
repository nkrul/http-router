package koncept.http.web.requestfilter

import koncept.http.web.context.RequestContext
import koncept.http.web.RequestContextMutator


//trait InboundFilter[T <: InboundFilter[T]] {
//  def accepts(rc: RequestContext, onAccept: RequestContextMutator): Boolean
//  //  def merge[T <: InboundFilter](other: T): InboundFilter
//  def merge(other: T): T
//}
trait InboundFilter {
  def accepts(rc: RequestContext[_], onAccept: RequestContextMutator): Boolean
    def merge(other: InboundFilter): InboundFilter
//  def merge[T <: InboundFilter](other: T): InboundFilter
}

case class UrlFilter(url: String)
  extends InboundFilter
  with UrlParsingFunctions {
  var fragments: List[UrlFramentFilter] = Nil
  val urlPath = url
  var fragment = head(urlPath)
  var remaining = tail(urlPath)
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

  def accepts(rc: RequestContext[_], onAccept: RequestContextMutator): Boolean = {
    var acceptable = true;
    for (fragmentFilter <- fragments) {
      if (acceptable) //TODO: The URL needs to be handled better
        acceptable = fragmentFilter.accepts(onAccept)
    }
    acceptable && onAccept.url.equals("")
  }

  def merge(other: InboundFilter): UrlFilter = other match {
    case urlFilter: UrlFilter => new UrlFilter(url + urlFilter.url)
  }
}

trait UrlParsingFunctions {
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
}

trait UrlFramentFilter extends UrlParsingFunctions {
  def accepts(onAccept: RequestContextMutator): Boolean

}
case class StaticUrlFragmentFilter(fragment: String) extends UrlFramentFilter {
  def accepts(onAccept: RequestContextMutator): Boolean = {
    if (head(onAccept.url).equals(fragment)) {
      onAccept.url = tail(onAccept.url)
      true
    } else {
      false
    }
  }
}
case class AnyUrl() extends UrlFramentFilter {
  def accepts(onAccept: RequestContextMutator): Boolean = {
    onAccept.addParameter("**", onAccept.url)
    onAccept.url = ""
    true
  }
}
case class AnyUrlFragment() extends UrlFramentFilter {
  def accepts(onAccept: RequestContextMutator): Boolean = {
    val fragment = head(onAccept.url)
    //    onAccept.url = onAccept.url.substring(fragment.length())
    onAccept.addParameter("*", fragment)
    onAccept.url = tail(onAccept.url)
    true
  }
}
case class UrlFragmentAsProperty(propertyName: String) extends UrlFramentFilter {
  def accepts(onAccept: RequestContextMutator): Boolean = {
    val fragment = head(onAccept.url)
    onAccept.addParameter(propertyName, fragment)
    onAccept.url = tail(onAccept.url)
    true
  }
}

class RequestMethodFilter(val methods: Seq[String]) extends InboundFilter {
  def accepts(rc: RequestContext[_], onAccept: RequestContextMutator): Boolean = {
    var acceptable = false
    for (method <- methods)
      acceptable |= rc.exchange.getRequestMethod.equals(method)
    acceptable
  }
  def merge(other: InboundFilter): RequestMethodFilter = other match {
    case requestMethodFilter: RequestMethodFilter => new RequestMethodFilter(methods ++ requestMethodFilter.methods)
  }
}
object RequestMethodFilter {
  def apply(methods: String*) = new RequestMethodFilter(methods)
}

case class SessionFilter(sessionExists: Boolean = true) extends InboundFilter {
  def accepts(rc: RequestContext[_], onAccept: RequestContextMutator) = {
    rc.httpSession.isDefined == sessionExists
  }
  def merge(other: InboundFilter): SessionFilter = throw new UnsupportedOperationException
}