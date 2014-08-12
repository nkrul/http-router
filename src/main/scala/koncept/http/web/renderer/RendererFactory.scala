package koncept.http.web.renderer

import koncept.http.web.response.WebResponse
import koncept.http.web.context.RequestContext

trait RendererFactory {

  def renderer() : Renderer;
    
}