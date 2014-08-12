package koncept.http.web.renderer.freemarker

import java.io.FileNotFoundException
import java.util.HashMap
import java.util.Map
import com.sun.net.httpserver.HttpExchange
import freemarker.template.Configuration
import koncept.http.web.renderer.Renderer
import koncept.http.web.renderer.RendererFactory
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.OutputStream



class FreeMarkerRenderer extends RendererFactory {
  
   var fmConfig = new Configuration
  //  fmConfig.setStrictSyntaxMode(false)
  fmConfig.setObjectWrapper(new ScalaObjectWrapper)
  fmConfig.setTemplateLoader(new ClassPathTemplateLoader)
  
  def config(fmConfig: Configuration) {
     this.fmConfig = fmConfig
   }
   
   def config() : Configuration = {
     fmConfig
   }
  
  def renderer() : Renderer = {
    return new Renderer() {
      @throws(classOf[FileNotFoundException])
      def render(
        responseCode: Int,
        template: String,
        exchange: HttpExchange,
        rootMap: Map[String, Any],
        mimeType: String = "text/html"
      ) {
        if (mimeType != null)
          exchange.getResponseHeaders().add("Content-Type", mimeType)
        val freeMarkerTemplate = fmConfig.getTemplate(template) //can throw a FileNofFoundException
        val out = new OutputStreamWriter(exchange.getResponseBody())
        exchange.sendResponseHeaders(responseCode, 0)
        freeMarkerTemplate.process(rootMap, out)
        out.flush
      }
      
      @throws(classOf[IOException])
      def render(
        template: String, 
        out: OutputStream,
        rootMap: Map[String, Any] //java map... for better or worse
      ) {
         val freeMarkerTemplate = fmConfig.getTemplate(template) //can throw a FileNofFoundException
         val outWriter = new OutputStreamWriter(out)
         freeMarkerTemplate.process(rootMap, outWriter)
         outWriter.flush
      }

    }
  }
  // http://freemarker.sourceforge.net/docs/pgui_quickstart_createdatamodel.html
  
}