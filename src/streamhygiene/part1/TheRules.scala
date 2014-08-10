/*
 * Originally (c) 2014 Dmitry Leskov, http://www.dmitryleskov.com
 * Released into the public domain under the Unlicense, http://unlicense.org
 * 
 * Reducing maximum heap size (-Xmx) setting will cause the failing tests
 * to fail sooner, reducing the overall run time.
 */

package streamhygiene
package part1
import scala.annotation.tailrec
import Test._

object TheRules extends AutoConfig with StreamConsumers {

    def input = Stream.continually(1) take problemSize
  
/* 
 *  Rule #1. Define streams using 'def' and never store them in 'val's.
 */
    test("input.length"){
      input.length
    }     
    test("memoizedInput.length"){                                 // OOM
      val memoizedInput = input
      memoizedInput.length
    }     
    
/*
 *  Rule #2: Consume streams in tail-recursive functions.
 */    
    test("@tailrec sum(input)"){
      @tailrec
      def sum(xs: Stream[Int], z: Int = 0): Int = 
        if (xs.isEmpty) z else sum(xs.tail, z + xs.head)
      sum(input)
    }  
    
/*
 *  Rule #3. Pass streams around via by-name parameters. 
 */
    test("sum(input)"){                                           // OOM
      def sum(xs: Stream[Int]): Int = {
        @tailrec
        def loop(acc: Int, xs: Stream[Int]): Int =
          if (xs.isEmpty) acc else loop(acc+xs.head, xs.tail)
        loop(0, xs)
      }
      sum(input)
    }                               
    test("sum(=> input)"){
      def sum(xs: => Stream[Int]): Int = {
        @tailrec
        def loop(acc: Int, xs: Stream[Int]): Int =
          if (xs.isEmpty) acc else loop(acc+xs.head, xs.tail)
        loop(0, xs)
      }
      sum(input)
    }

/*
 *  Rule #3a. Corollary: When defining stream-consuming functions in traits,
 *            wrap them in methods accepting streams as by-name parameters.
 */
    test("traitSumTailRec(input)"){traitSumTailRec(input)}        // OOM
    test("traitSumByName(input)"){traitSumByName(input)}
    
/*
 *  Rule #4. Do not pattern match against streams outside the tail-recursive 
 *           consuming functions. 
 */    
    test("sumPatMat(input)"){                                     // OOM
      def sumPatMat(xs: => Stream[Int]): Int = {
        @tailrec
        def loop(acc: Int, xs: Stream[Int]): Int =
          if (xs.isEmpty) acc else loop(acc+xs.head, xs.tail)
        xs match {
          case Stream.Empty => 0
          case x #:: Stream.Empty => x
          case x #:: ys => loop(x, ys)
        }
      }
      sumPatMat(input)
    }  
    test("sumPatMatInner(input)"){
      def sumPatMatInner(xs: => Stream[Int]): Int = {
        @tailrec
        def loop(acc: Int, xs: Stream[Int]): Int =
          xs match {
            case Stream.Empty => acc
            case y #:: ys => loop(acc + y, ys)
          }
        loop(0, xs)
      }
      sumPatMatInner(input)
    }  

/*
 *  Rule #5. Only call the eagerly evaluated `Stream` methods 
 *           that are marked as *"optimized for GC"*. 
 *   
 *  length, foldLeft, reduceLeft and foreach are GC-safe:
 */
    test("input.length"){input.length}     
    test("input.foldLeft(0)(_ + _)"){input.foldLeft(0)(_ + _)}     
    test("input.reduceLeft(_ + _)"){input.reduceLeft(_ + _)}     
    test("{var sum = 0; input.foreach(x => sum += x); sum}"){
      var sum = 0; input.foreach(x => sum += x); sum
    }     
/*
 *  whereas /:, forall, exists, find, sum and even last are not
 */    
    test("(0 /: input)(_ + _)"){(0 /: input)(_ + _)}         // OOM
    test("input forall (_ == 1)"){input forall (_ == 1)}     // OOM
    test("input exists (_ != 1)"){input exists (_ != 1)}     // OOM
    test("input find (_ != 1)"){input find (_ != 1)}         // OOM 
    test("input.sum"){input.sum}                             // OOM
    test("input.last"){input.last}                           // OOM
    test("input.distinct"){input.distinct}
}