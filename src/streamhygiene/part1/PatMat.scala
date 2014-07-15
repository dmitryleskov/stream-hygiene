/*
 * Originally (c) 2014 Dmitry Leskov, http://www.dmitryleskov.com
 * Released into the public domain under the Unlicense, http://unlicense.org
 * 
 * Decompile to see what pattern matching compiles to.
 */

package streamhygiene
package part1

object PatMat {
    def input = Stream.continually(0) take 10
    def f(x: Int, xs: => Stream[Int]) = {}
    def g(xs: => Stream[Int]) = {}

/*
 *  The following match operator:
 */
    input match {
      case x #:: xs => f(x, xs)
      case _ => println("no match")
    }
/*
 *  Is equivalent to:
 */    
    val localOption2: Option[(Int, Stream[Int])] = Stream.#::.unapply(input)
    if (localOption2.isEmpty) println("no match")
    else {val x = localOption2.get._1; val xs = localOption2.get._2; f(x, xs)}
/*
 *  Notice how a successful match results in the creation of two vals.
 */

/*
 *  Even if you don't supply variable names for the stream's head and tail...
 */
    input match {
      case s @ (_ #:: _) => g(s)
      case _ => println("no match")
    }
/*
 *  ...there is also a val holding the result of the #::.unapply() call.
 */
}
