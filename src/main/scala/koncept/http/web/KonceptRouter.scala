package koncept.http.web

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

import com.sun.net.httpserver.spi.HttpServerProvider

import koncept.http.web.auth.SessionCookieAuthenticator
import koncept.http.web.auth.SessionCookieAuthenticatorCleaner
import koncept.http.web.delegate.DispatcherDelegate
import koncept.http.web.delegate.FilterDelegate
import koncept.http.web.httpfilter.ParameterFilter

trait KonceptRouter[R] {
  def createPerRequestResources(): RequestResources[R]
  var sessionMinutesTimeout: Int = 20
  
  def apply(handler : RequestHandler[R]) : KonceptRouter[R] = {
    addHandler(handler)
    this
  }
  
  def apply(responseCode: Int, handler : OutputHandler[R]) : KonceptRouter[R] = {
    defineHandler(responseCode, handler)
    this
  }
  
  private var handlers: List[RequestHandler[R]] = Nil
  def addHandler(handler : RequestHandler[R]): Unit = {
    handlers ::= handler
  }
  
  private var outputHandlers: Map[Int, OutputHandler[R]] = Map()
  def defineHandler(responseCode: Int, handler: OutputHandler[R]) {
    outputHandlers += (responseCode -> handler)
  }
  
  def start(address: InetSocketAddress = new InetSocketAddress(8080), backlog:Integer = 0, provider: HttpServerProvider = HttpServerProvider.provider()) {
    val httpServer = provider.createHttpServer(address, backlog)
    
    val dispatcher: HttpRequestDispatcher[R] = new HttpRequestDispatcher(createPerRequestResources)
    for(handler <- handlers)
      dispatcher.addHandler(handler)
      
    for(handlerSpec <- outputHandlers) {
      dispatcher.defineHandler(handlerSpec._1, handlerSpec._2)
    }
    
    val httpContext = httpServer.createContext("/", DispatcherDelegate(dispatcher))
    
    val authenticator = new SessionCookieAuthenticator(minutesTimeout = sessionMinutesTimeout)
    httpContext.setAuthenticator(authenticator)
    httpContext.getFilters().add(FilterDelegate(new ParameterFilter))
    
    httpServer.setExecutor(Executors.newCachedThreadPool)
    httpServer.start()
    
    //run session cleanup every minute
    val scheduledThreads = Executors.newScheduledThreadPool(1)
    scheduledThreads.scheduleWithFixedDelay(new SessionCookieAuthenticatorCleaner(authenticator, httpContext), 0, 1, TimeUnit.MINUTES)
  }
  
}