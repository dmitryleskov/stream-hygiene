/*
 * Originally (c) 2014 Dmitry Leskov, http://www.dmitryleskov.com
 * Released into the public domain under the Unlicense, http://unlicense.org
 */

package streamhygiene

class AutoConfig extends App {
  val problemSize = 
    try {
      if (args.length == 0) {
        val heapSize = Runtime.getRuntime.maxMemory();
        if (heapSize > Int.MaxValue) {
          println("Heap size too big, set smaller heap using -Xmx")
          sys.exit(1)
        }
        // (Very) conservatively estimate that a cons cell takes at least 16 bytes
        (heapSize.toInt / 16000000 + 1) * 1000000
      } 
      else if (args.length == 1) args(0).toInt
      else throw new Error("Too many arguments")
    } catch {
      case t: Throwable => println(t.getMessage); sys.exit(1)
    }
  println("Problem size: " + problemSize)
}
