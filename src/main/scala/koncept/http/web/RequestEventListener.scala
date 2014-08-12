package koncept.http.web

import koncept.http.web.response.WebResponse
import koncept.http.web.context.RequestContext

trait RequestEventListener[R] {

  def before(rc: RequestContext[R]): Boolean
  
  def after(response: WebResponse);
  
  def error(t: Throwable);
  
}