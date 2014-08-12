package koncept.http.web.response

class WebResponse(
    val responseCode: Int,
    val responseData: String,
    val details: scala.collection.Map[String, Any]
    ) {
  
  def this(responseCode: Int) = this(responseCode, null, null)
  def this(responseCode: Int, responseData: String) = this(responseCode, responseData, null)
  def this(responseData: String) = this(200, responseData, null)
  def this(responseData: String, data: scala.collection.Map[String, Any]) = this(200, responseData, data)
}