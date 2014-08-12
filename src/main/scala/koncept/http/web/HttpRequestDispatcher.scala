package koncept.http.web

import scala.collection.immutable.List
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import koncept.http.web.context.RequestContext
import koncept.http.web.renderer.RendererFactory
import koncept.http.web.renderer.freemarker.FreeMarkerRenderer
import koncept.http.web.response.WebResponse

class HttpRequestDispatcher[R](
  val requestResources: RequestResources[R],
  val rendererFactory: RendererFactory = new FreeMarkerRenderer()
  ) extends HttpHandler {

  var handlers: List[RequestHandler[R]] = Nil
  def addHandler(handler: RequestHandler[R]): Unit = {
    handlers ::= handler
  }
  
  var outputHandlers: Map[Int, OutputHandler[R]] = Map()
  def defineHandler(responseCode: Int, handler: OutputHandler[R]) {
    outputHandlers += (responseCode -> handler)
  }
  
  //seed some sensible defaults
  defineHandler(0, new NoResponse[R]())
  defineHandler(200, new TemplatedResponse[R]())
  defineHandler(301, new RedirectResponse[R]())
  defineHandler(302, new RedirectResponse[R]())
  defineHandler(404, new ErrorCodeResponseResponse[R]())
  defineHandler(500, new ErrorCodeResponseResponse[R]()) 

  def handle(exchange: HttpExchange): Unit = {
    val rc = new RequestContext[R](exchange, requestResources.requestStart, rendererFactory)
    var filters: List[RequestEventListener[R]] = Nil
    try {
      var endpointEvent: Option[EndpointEvent[R]] = None
      var filterEvents: List[FilterEvent[R]] = Nil
      //list of filters

      for (handler <- handlers) {
        if (endpointEvent.isEmpty) {
          endpointEvent = handler.applies(rc, new RequestContextMutator(rc.url))
        }
        filterEvents ++= handler.listeners(rc, new RequestContextMutator(rc.url)); //rcm is thrown away though... hmm
      }

      if (endpointEvent.isDefined)
        endpointEvent.get.prepare(rc)

      //apply onstart filters....
      var performRequest = true;
      for (filterEvent <- filterEvents) {
        if (performRequest) {
          val filter = filterEvent.apply
          filters ::= filter
          performRequest = filter.before(rc)
        }
      }

      val webResponse: WebResponse =
        if (endpointEvent.isDefined && performRequest) {
          endpointEvent.get.respond(rc)
        } else {
          new WebResponse(404)
        }
      
      //must have the 'response code' defined
      outputHandlers(webResponse.responseCode).handle(rc, webResponse)

      for (filter <- filters) {
        filter.after(webResponse)
      }

    } catch {
      case t: Throwable => {
        for (filter <- filters) try {
          filter.error(t)
        } catch {
          case t: Throwable => t.printStackTrace()
        }
      }
    } finally {
      requestResources.requestEnd(rc.resources)
      exchange.getResponseBody().flush()
      exchange.getResponseBody().close()
      //don't close the exchange, Keep-Alive is used.   (??)
      //this is handled by the http server
      //      exchange close //TODO = need to change this to be a more manual 'forced' close
    }
  }

}