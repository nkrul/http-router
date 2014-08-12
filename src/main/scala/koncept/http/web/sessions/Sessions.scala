package koncept.http.web.sessions

import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import com.sun.net.httpserver.HttpPrincipal
import java.util.HashSet
import scala.collection.JavaConversions._
import java.util.concurrent.TimeUnit

class Sessions {

  val map: ConcurrentHashMap[String, Session] = new ConcurrentHashMap[String, Session]();
  
  def create(principal: HttpPrincipal): Session = {
   val session = new Session(principal, generateSessionId)
   map.put(session.id, session)
   session
  }
  
  def get(id: String) : Session = {
    //needs to handle expiretimes here?
    val session = map.get(id)
    if (session != null)
      session.touch
    session
  }
  
  def destroy(id: String) {
    map.remove(id)
  }
  
  def clear() {
    map.clear()
  }
  
  def cleanup(minutesTimeout: Int) {
    //do cleanup!!
    val minimumStartTime = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutesTimeout);
    for(key <- new HashSet[String](map.keySet())) {
      val session = map.get(key)
      if (session.lastTouch < minimumStartTime)
        destroy(key)
    }
  }
  
  //need some king of housekeeping here
  
  private def generateSessionId(): String = {
    return (System.currentTimeMillis() + System.nanoTime()).toString.reverse
  }
  
}
object Sessions {
  val sessionsKey = "_sessions";
  def apply(httpContextAttributes: java.util.Map[String,Object]) = {
    httpContextAttributes.get(sessionsKey) match {
      case s: Sessions => {s}
      case null =>{
        synchronizedCreateIfRequired(httpContextAttributes)
      }
    }
  }
  
  //mmm... double checked locking in scala...
  private def synchronizedCreateIfRequired(httpContextAttributes: java.util.Map[String,Object]) : Sessions = {
    this.synchronized {
      httpContextAttributes.get(sessionsKey) match {
      case s: Sessions => {s}
      case null => {
        val s = new Sessions();
        httpContextAttributes.put(sessionsKey, s)
        s
        }
      }
    }
  }
}