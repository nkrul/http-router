package koncept.http.web.renderer.freemarker

import java.io.InputStreamReader
import java.net.URL

import freemarker.cache.TemplateLoader

class ClassPathTemplateLoader extends TemplateLoader {

  def findTemplateSource(name: String) : Object = {
//    FileSystemLocator.resourcesLocation
//    println("Looking for template /templates/" + name)
    val url = getClass().getResource("/templates/" + name)
//    println("url is " + url)
    url
  }
  
  def getLastModified(o: Object) : Long = {
    0
  }
  
  def closeTemplateSource(o: Object) {
//    println("closeTemplateSource " + o)
  }
  
  def getReader(o: Object, encoding: String) : java.io.Reader = {
    return o match {
      case url: URL => new InputStreamReader(url.openStream)
    }
  }
  
}