/*
 * Originally (c) 2014 Dmitry Leskov, http://www.dmitryleskov.com
 * Released into the public domain under the Unlicense, http://unlicense.org
 */

package streamhygiene
package part2
import Test._

object Part2 extends InputGenerator {
/*
 * It looks like the imperative version of sum() does not leak memory
 */
  test("imperative sum(input)"){  
    def sum(xs: Stream[Int]): Int = {
      var scan = xs
      var res = 0
      while (!scan.isEmpty) {
        res += scan.head
        scan = scan.tail
      }
      res
    }
    sum(input)
  }

/*
 * In fact, Stream.sum() also stops leaking memory after HotSpot kicks in
 */
  test("input.sum"){input.sum}
  test("for (i <- 1 to 10000){(input take 10).sum}"){
    for (i <- 1 to 10000) {
      (input take 10).sum
    }
  }
  test("input.sum"){input.sum}
  
/*
 * But Rules #1 holds, so Rule #4 holds too, albeit in the form
 * "When pattern matching on streams, make sure to not use pattern variables
 * after the call of a stream consuming function."
 */  
  test("tailAvg(input)"){
    def tailAvg(xs: Stream[Int]): Option[Int] = {
      xs match {
        case Stream.Empty => None
        case y #:: Stream.Empty => None
        case y #:: ys => Some(ys.sum / ys.length)
      }
    }
    tailAvg(input)
  }  
}
