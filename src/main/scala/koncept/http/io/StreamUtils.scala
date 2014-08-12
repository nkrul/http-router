package koncept.http.io

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream

class StreamUtils {

  @throws(classOf[IOException])
  def copy(in: InputStream, out: OutputStream) = {
    val buffer = new Array[Byte](1024)
    Iterator.continually(in.read(buffer))
      .takeWhile(_ != -1)
      .foreach { out.write(buffer, 0, _) }
    out.flush()
  }
  
  
  @throws(classOf[IOException])
  def read(in: InputStream, op: String => Unit) = {
    val reader = new BufferedReader(new InputStreamReader(in))
    Iterator.continually(reader.readLine())
      .takeWhile(_ != null)
      .foreach { op(_) }
    reader.close();
  }
  
  @throws(classOf[IOException])
  def foldLeft[T] (in: InputStream)(initial: T)(op: (T, String) => T) : T = {
    val reader = new BufferedReader(new InputStreamReader(in))
    val result = Iterator.continually(reader.readLine())
      .takeWhile(_ != null)
      .foldLeft(initial)(op)
      //.foreach { operation(_) }
    reader.close();
    result
  }

  
}