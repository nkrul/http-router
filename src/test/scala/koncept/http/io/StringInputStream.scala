package koncept.http.io

import java.io.InputStream
import java.io.ByteArrayInputStream

class StringInputStreamFactory  {
  val charset = "UTF-8"
  
  def apply(s: String): InputStream = {
    new ByteArrayInputStream(toBytes(s))
  }

  def toBytes(value: String): Array[Byte] = {
    value getBytes charset
  }
}