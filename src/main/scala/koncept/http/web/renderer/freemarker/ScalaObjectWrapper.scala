package koncept.http.web.renderer.freemarker

import java.lang.reflect.Method
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.mapAsScalaMap
import freemarker.template.ObjectWrapper
import freemarker.template.TemplateCollectionModel
import freemarker.template.TemplateHashModel
import freemarker.template.TemplateModel
import freemarker.template.TemplateModelException
import freemarker.template.TemplateModelIterator
import freemarker.template.TemplateScalarModel
import freemarker.template.TemplateBooleanModel

class ScalaObjectWrapper
	extends ScalaTemplateModel
	with ObjectWrapper {
}

trait ScalaTemplateModel {
  @throws(classOf[TemplateModelException])
  def wrap(obj: Any): TemplateModel = {
    if (obj == null)
      return null
    obj match {
      case m: scala.collection.Map[Any, Any] => new MapModel(m)
      case m: java.util.Map[Any, Any] => new MapModel(m)
      //traversableonce would pick up maps as well if they weren't already handled
      case l: TraversableOnce[Any] => new ListTemplateModel(l)
      case l: java.util.List[_] => new ListTemplateModel(l)
      case a: Array[Any] => new ListTemplateModel(a)
      case s: Long => new ScalaTemplateScalarModel(s)
      case s: Integer => new ScalaTemplateScalarModel(s)
      case s: String => new ScalaTemplateScalarModel(s)
//      case v: Enumeration => new ScalaTemplateScalarModel(v) //scala enum values ?
      case b: Boolean => new ScalaTemplateBooleanModel(b)
      case x : Any => new MethodExecutingWrapper(x)
    }
  }
}

class ScalaTemplateScalarModel(obj: Any)
	extends TemplateScalarModel {
  def getAsString(): String = {
    obj toString
  }
}

class ScalaTemplateBooleanModel(obj: Boolean)
	extends TemplateBooleanModel {
  def getAsBoolean() : Boolean = {
    obj
  }
}

class MapModel(map: scala.collection.Map[Any, Any])
	extends ScalaTemplateModel
	with TemplateCollectionModel
	with TemplateHashModel {
  def isEmpty(): Boolean = {
    map.isEmpty
  }
  def get(name: String) : TemplateModel = {
    map.get(name) match {
      case Some(value) => return wrap(value)
      case _ => {}
    }
    null
  }
  
  //return the *KEYSET*
  def iterator: TemplateModelIterator = {
    new IteratorTemplateModel(map.keysIterator)
  }
}

class ListTemplateModel(traversable: TraversableOnce[Any])
	extends TemplateCollectionModel {
  def iterator: TemplateModelIterator = {
    new IteratorTemplateModel(traversable.toIterator)
  }
}

class IteratorTemplateModel(iterator: Iterator[Any])
	extends ScalaTemplateModel
	with TemplateModelIterator {
  def hasNext() : Boolean = {
    iterator.hasNext
  }
  def next(): TemplateModel = {
    wrap(iterator.next)
  }
}

class MethodExecutingWrapper(obj: Any)
	extends ScalaTemplateModel
	with TemplateHashModel
	with TemplateScalarModel {
  def isEmpty(): Boolean = {
    false
  }
  
  def getAsString(): String = {
    obj.toString
  }
  
  def get(name: String) : TemplateModel = {
    var foundMethod: Option[Method] = None
    for (method <- obj.getClass().getMethods()) {
        if (foundMethod.isEmpty 
            && method.getName().equals(name) 
            && method.getParameterTypes().length == 0)
          foundMethod = Some(method);
    }
    if (foundMethod.isDefined)
      return wrap(foundMethod.get.invoke(obj))
    
    //return null or throw exception??
    throw new RuntimeException(obj + " does not contain " + name)
  }
}