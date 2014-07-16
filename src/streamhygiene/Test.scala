/*
 * Originally (c) 2014 Dmitry Leskov, http://www.dmitryleskov.com
 * Released into the public domain under the Unlicense, http://unlicense.org
 */

package streamhygiene

object Test {
  def test(desc: String)(f: => Any): Unit = {
    print(desc)
    try {
      println(" = " + f)
    }
    catch {
      case oom: OutOfMemoryError => println(" throws OutOfMemoryError") 
    }
  }
}
