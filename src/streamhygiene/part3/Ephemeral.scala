/*
 * Originally (c) 2014 Dmitry Leskov, http://www.dmitryleskov.com
 * Released into the public domain under the Unlicense, http://unlicense.org
 */

package streamhygiene
package part3
import scala.annotation.tailrec
import scalaz._
import scalaz.EphemeralStream._
import Test._

trait EphemeralStreamConsumers {
  def traitSum(xs: EphemeralStream[Int]): Int = {
    @tailrec
    def loop(acc: Int, xs: EphemeralStream[Int]): Int =
      if (xs.isEmpty) acc else loop(acc+xs.head(), xs.tail())
    loop(0, xs)
  }

  @tailrec
  final def traitSumTailRec(xs: EphemeralStream[Int], z: Int = 0): Int = {
    if (xs.isEmpty) z else traitSumTailRec(xs.tail(), z + xs.head())
  }
  
  def traitSumImperative(xs: EphemeralStream[Int]): Int = {
    var scan = xs
    var res = 0
    while (!scan.isEmpty) {
      res += scan.head()
      scan = scan.tail()
    }
    res
  }
}

object Ephemeral extends AutoConfig with EphemeralStreamConsumers {
 /* 
  *  No Rule #1: Storing an EphemeralStream in a 'val' is no problem.
  */
  def ones: EphemeralStream[Int] = 1 ##:: ones
  val input = ones take problemSize
  test("input.length"){
    input.length
  }     
 /* 
  *  No Rule #2: Functions consuming EphemeralStreams do not need to be tail-recursive.
  */
  test("Imperative sum(input)"){
    def sum(xs: EphemeralStream[Int]): Int = {
      var scan = xs
      var res = 0
      while (!scan.isEmpty) {
        res += scan.head()
        scan = scan.tail()
      }
      res
    }
    sum(input)
  }

 /* 
  *  No Rule #3: No need to use by-name parameters in intermediate functions.
  */
  test("sum(input) holding reference to its parameter"){
    def sum(xs: EphemeralStream[Int]): Int = {
      @tailrec
      def loop(acc: Int, xs: EphemeralStream[Int]): Int =
        if (xs.isEmpty) acc else loop(acc+xs.head(), xs.tail())
      loop(0, xs.tail()) + xs.head()
    }
    sum(input)
  }

 /* 
  *  No Rule #3 corollary: No need to use by-name parameters in 
  *                        consuming functions defined in traits.
  */
  test("traitSum(input)"){traitSum(input)}
  test("traitSumTailRec(input)"){traitSumTailRec(input)}
  test("traitSumImperative(input)"){traitSumImperative(input)}
  
/*
 *   No Rule #4: Pattern matching on EphemeralStreams is okay.
 *               There is no equivalent of Stream.Empty, though.
 */  
  test("sumPatMat(input)"){
    def sumPatMat(xs: EphemeralStream[Int]): Int = {
      @tailrec
      def loop(acc: Int, xs: EphemeralStream[Int]): Int =
        xs match {
          case x ##:: xs => loop(acc+x, xs)
          case _ => acc
        }
      xs match {
        case h ##:: t => loop(0, xs)
        case _ => 0
      }
    }
    sumPatMat(input)
  }
  
  test("average(input)"){
    def average(xs: EphemeralStream[Int]): Option[Int] = {
      xs match {
        case y ##:: ys => Some(ys.sum / ys.length)
        case _ => None
      }
    }
    average(input)
  }  

/*
 *  No Rule $5. All eager stream-consuming methods work (if the stream is finite).
 *              Not all Stream methods are available, however, and 
 *              EphemeralStream's own methods need special treatment.
 */  
  def plus(x: => Int)(y: => Int) = x+y
  test("input.foldLeft(0)(plus)"){input.foldLeft(0)(plus)}     
  test("input.reduceLeft(_ + _)"){input.reduceLeft(_ + _)}     
  test("{var sum = 0; input.foreach(x => sum += x); sum}"){
    var sum = 0; input.foreach(x => sum += x); sum
  }     
  test("input.sum"){input.sum}
  test("(0 /: (input))(_ + _)"){(0 /: (input))(_ + _)}
  test("input forall (_ == 1)"){input forall (_ == 1)}
  test("input exists (_ != 1)"){input exists (_ != 1)}
  test("input find (_ != 1)"){input find (_ != 1)}
}