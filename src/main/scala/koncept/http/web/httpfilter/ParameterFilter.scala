package koncept.http.web.httpfilter

import java.io.InputStream
import com.sun.net.httpserver.Filter
import com.sun.net.httpserver.HttpExchange
import koncept.http.io.StreamUtils


class ParameterFilter extends Filter {

  def description(): String = {
    "parameterFilter"
  }

  //supports splitting URL and BODY params... but... why bother, its not useful
  def doFilter(exchange: HttpExchange, chain: Filter.Chain) = {
    exchange.setAttribute("parameterFilter", this)
//    val urlParams = extractUrlParams(exchange, Map())
    var parameters = extractUrlParams(exchange.getRequestURI().getRawQuery(), Map())

    exchange.getRequestMethod() match {
      case "GET" => {}
      case "PUT" => { parameters = extractBodyParams(exchange.getRequestBody(), parameters) }
      case "POST" => { parameters = extractBodyParams(exchange.getRequestBody(), parameters) }
      case s: String => { Console.println("unhandled http type: " + s) }
    }

    //    exchange.setAttribute("url-params", urlParams)
    //    exchange.setAttribute("body-params", bodyParams)
    exchange.setAttribute("parameters", parameters)
    
    chain.doFilter(exchange)
  }

  def extractUrlParams(query: String, map: Map[String, List[String]]): Map[String, List[String]] = {
    if (query == null) return Map()
    return splitParams(Map(), query)
  }

  def extractBodyParams(in: InputStream, map: Map[String, List[String]]): Map[String,  List[String]] = {
      return new StreamUtils().foldLeft[Map[String,  List[String]]](in)(map)(splitParams)
  }
  
//  def extractBodyParams(in: InputStream, map: Map[String, String]): Map[String, String] = {
//      return new StreamUtils().foldLeft[Map[String, String]](in)(map)(splitParams)
//  }

  //matches the foldLeft concept for nvp's
  def splitParams(map: Map[String, List[String]], queryString: String): Map[String, List[String]] = {
    if (queryString == null || queryString.equals(""))
      return map
    var found: Map[String, List[String]] = map
    queryString.split("&").foreach(param => {
      //Console.println("input param: " + param)
      found = appendParamToMap(found, java.net.URLDecoder.decode(param, "UTF-8")) //need to URL decode
    })
    return found
  }
  
  def appendParamToMap(map: Map[String, List[String]], param: String): Map[String, List[String]] = {
    val splitIndex = param.indexOf("=")
    val key: String = if (splitIndex == -1) "" else param.substring(0, splitIndex)
    var value: String = if (splitIndex == -1) param else param.substring(splitIndex + 1)
    var list: List[String] = map.getOrElse(key, Nil)
    list ::= value 
    return map + (key -> list)
  }
}