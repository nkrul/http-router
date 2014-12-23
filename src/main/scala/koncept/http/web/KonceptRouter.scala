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
  
  private val dispatcher: HttpRequestDispatcher[R] = new HttpRequestDispatcher(createPerRequestResources)
  private val authenticator = new SessionCookieAuthenticator
  
  def sessionMinutesTimeout(minutesTimeout: Int) {
    authenticator.minutesTimeout = minutesTimeout
  }

  def apply(handler : RequestHandler[R]) : KonceptRouter[R] = {
    addHandler(handler)
    this
  }
  
  def apply(responseCode: Int, handler : OutputHandler[R]) : KonceptRouter[R] = {
    defineHandler(responseCode, handler)
    this
  }
  
  def addHandler(handler : RequestHandler[R]): Unit = {
    dispatcher.addHandler(handler)
  }
  
  def defineHandler(responseCode: Int, handler: OutputHandler[R]) {
    dispatcher.defineHandler(responseCode, handler)
  }
  
  def start(address: InetSocketAddress = new InetSocketAddress(8080), backlog:Integer = 0, provider: HttpServerProvider = HttpServerProvider.provider()) : Stopper = {
    val httpServer = provider.createHttpServer(address, backlog)
    val httpContext = httpServer.createContext("/", DispatcherDelegate(dispatcher))
    
    httpContext.setAuthenticator(authenticator)
    httpContext.getFilters().add(FilterDelegate(new ParameterFilter))
    
    val executorService = Executors.newCachedThreadPool
    
    httpServer.setExecutor(executorService)
    httpServer.start()
    
    //run session cleanup every minute
    val scheduledThreads = Executors.newScheduledThreadPool(1)
    scheduledThreads.scheduleWithFixedDelay(new SessionCookieAuthenticatorCleaner(authenticator, httpContext), 0, 1, TimeUnit.MINUTES)
    new Stopper {
      def stop() {
        httpServer.stop(1000)
        executorService.shutdown()
        scheduledThreads.shutdown()
      }
    }
  }
  
}

trait Stopper {
  def stop
}