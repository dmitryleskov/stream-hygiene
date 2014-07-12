/*
 * Originally (c) 2014 Dmitry Leskov, http://www.dmitryleskov.com
 * Released into the public domain under the Unlicense, http://unlicense.org
 */

package streamhygiene
import scala.annotation.tailrec
import Test._

trait StreamConsumers {
  @tailrec
  final def traitSumTailRec(xs: Stream[Int], z: Int = 0): Int = {
    if (xs.isEmpty) z else traitSumTailRec(xs.tail, z + xs.head)
  }
  def traitSumImperative(xs: Stream[Int]): Int = {
    var scan = xs
    var res = 0
    while (!scan.isEmpty) {
      res += scan.head
      scan = scan.tail
    }
    res
  }
  def traitSumByName(xs: => Stream[Int]): Int = {
    @tailrec def loop(acc: Int, xs: Stream[Int]): Int =
      if (xs.isEmpty) acc else loop(acc+xs.head, xs.tail)
    loop(0, xs)
  }
}

object Main extends StreamConsumers {
  def ones: Stream[Int] = 1 #:: ones
   
  def sum(xs: Stream[Int]): Int = {
    @tailrec
    def loop(acc: Int, xs: Stream[Int]): Int =
      if (xs.isEmpty) acc else loop(acc+xs.head, xs.tail)
    loop(0, xs)
  }

  def sumByName(xs: => Stream[Int]): Int = {
    @tailrec
    def loop(acc: Int, xs: Stream[Int]): Int =
      if (xs.isEmpty) acc else loop(acc+xs.head, xs.tail)
    loop(0, xs)
  }
  
  @tailrec
  def sumTailRec(xs: Stream[Int], z: Int = 0): Int = 
    if (xs.isEmpty) z else sumTailRec(xs.tail, z + xs.head)

  def sumImperative(xs: Stream[Int]): Int = {
    var scan = xs
    var res = 0
    while (!scan.isEmpty) {
      res += scan.head
      scan = scan.tail
    }
    res
  }

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

  def sumPatMatInner(xs: => Stream[Int]): Int = {
    @tailrec
    def loop(acc: Int, xs: Stream[Int]): Int =
      xs match {
        case Stream.Empty => acc
        case y #:: ys => loop(acc + y, ys)
      }
    loop(0, xs)
  }

  def leaky(xs: Stream[Int]): Option[Double] = {
    xs match {
      case y #:: ys => Some(y + sumByName(ys) / ys.length)
      case _ => None
    }
  }

  
  def main(args: Array[String]): Unit = {
/*    
    def getStream: Stream[Int] = Stream.from(0) take 1000000
    
    def f(x: Int, xs: => Stream[Int]) = {}
    def g(xs: => Stream[Int]) = {}
    
    getStream match {
      case x #:: xs => f(x, xs)
      case _ => println("no match")
    }

    getStream match {
      case s @ (_ #:: _) => sumByName(s)
      case _ => println("no match")
    }
    val foo: Option[(Int, Stream[Int])] = Stream.#::.unapply(getStream)
    if (foo.isEmpty) println("No data to process")
    else {val x = foo.get._1; val xs = foo.get._2; f(x, xs)}
*/    
    
    test("(ones drop 1000000).head"){(ones drop 1000000).head}
    test("(ones take 1000000).length"){(ones take 1000000).length}     
    test("(ones take 1000000).foldLeft(0)(_ + _)"){(ones take 1000000).foldLeft(0)(_ + _)}     
    test("(ones take 1000000).reduceLeft(_ + _)"){(ones take 1000000).reduceLeft(_ + _)}     
    test("{var sum = 0; (ones take 1000000).foreach(x => sum += x); sum}"){
      var sum = 0; (ones take 1000000).foreach(x => sum += x); sum
    }     
    test("(ones take 1000000).sum"){(ones take 1000000).sum}     // OOM
    test("(0 /: (ones take 1000000))(_ + _)"){(0 /: (ones take 1000000))(_ + _)}     // OOM
    test("ones take 1000000 forall (_ == 1)"){ones take 1000000 forall (_ == 1)}
    test("ones take 1000000 exists (_ != 1)"){ones take 1000000 exists (_ != 1)}
    test("ones take 1000000 find (_ != 1)"){ones take 1000000 find (_ != 1)}
    test("(ones take 1000000).distinct"){(ones take 1000000).distinct}
    test("sum(ones take 1000000)"){sum(ones take 1000000)}  // OOM
    test("sumByName(ones take 1000000)"){sumByName(ones take 1000000)}  
    test("sumTailRec(ones take 1000000)"){sumTailRec(ones take 1000000)}  
    test("sumImperative(ones take 1000000)"){sumImperative(ones take 1000000)}  
    test("sumPatMat(ones take 1000000)"){sumPatMat(ones take 1000000)}  
    test("sumPatMatInner(ones take 1000000)"){sumPatMatInner(ones take 1000000)}  
    test("leaky(ones take 1000000)"){leaky(ones take 1000000)}  
    test("traitSumTailRec(ones take 1000000)"){traitSumTailRec(ones take 1000000)}  // OOM
    test("traitSumImperative(ones take 1000000)"){traitSumImperative(ones take 1000000)}
    test("traitSumByName(ones take 1000000)"){traitSumByName(ones take 1000000)}
  }
}

