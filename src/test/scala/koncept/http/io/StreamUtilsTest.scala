package koncept.http.io

import java.io.IOException

import org.scalatest.FlatSpec

class StreamUtilsTest extends FlatSpec {

  "Stream Utils" should "read lines in" in {
    val toStream = new StringInputStreamFactory
    val testString = "1\n2\n3 lines long"
    val counter = streamCounter
    new StreamUtils().read(toStream(testString), counter.onString)
    assert(counter.count == 3)
  }
  
  
  def streamCounter = new {
    var count = 0
    
    def onString(s: String) {
      count = count + 1
    }
    
  }
  
}