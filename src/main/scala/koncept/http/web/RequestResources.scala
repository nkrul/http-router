package koncept.http.web

trait RequestResources[R] {

  def requestStart(): R
  def requestEnd(resources: R) : Unit
  
}