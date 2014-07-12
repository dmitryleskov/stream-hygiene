/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package streamhygiene
import scala.annotation.tailrec
import scalaz._
import scalaz.EphemeralStream._

trait EphemeralStreamConsumers {
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
  def traitSumByName(xs: => EphemeralStream[Int]): Int = {
    @tailrec def loop(acc: Int, xs: EphemeralStream[Int]): Int =
      if (xs.isEmpty) acc else loop(acc+xs.head(), xs.tail())
    loop(0, xs)
  }
}

object Ephemeral extends EphemeralStreamConsumers {
  def ones: EphemeralStream[Int] = 1 ##:: ones
   
  def prefixSum(xs: => EphemeralStream[Int], n: Int): Int = {
    @tailrec
    def loop(acc: Int, xs: EphemeralStream[Int], n: Int): Int =
      if (n == 0 || xs.isEmpty) acc else loop(acc+xs.head(), xs.tail(), n-1)
    loop(0, xs, n)
  }

  def sum(xs: EphemeralStream[Int]): Int = {
    @tailrec
    def loop(acc: Int, xs: EphemeralStream[Int]): Int =
      if (xs.isEmpty) acc else loop(acc+xs.head(), xs.tail())
    loop(0, xs)
  }

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
  
  def sumByName(xs: => EphemeralStream[Int]): Int = {
    @tailrec
    def loop(acc: Int, xs: EphemeralStream[Int]): Int =
      if (xs.isEmpty) acc else loop(acc+xs.head(), xs.tail())
    loop(0, xs)
  }
  
  @tailrec
  def sumTailRec(xs: EphemeralStream[Int], z: Int = 0): Int = 
    if (xs.isEmpty) z else sumTailRec(xs.tail(), z + xs.head())

  def sumImperative(xs: EphemeralStream[Int]): Int = {
    var scan = xs
    var res = 0
    while (!scan.isEmpty) {
      res += scan.head()
      scan = scan.tail()
    }
    res
  }

  def leaky(xs: EphemeralStream[Int]): Option[Double] = {
    xs match {
      case y ##:: ys => Some(y + sum(ys) / ys.length)
      case _ => None
    }
  }
  
  def test(desc: String)(f: => Any): Unit = {
    print(desc)
    try {
      println(" = " + f)
    }
    catch {
      case oom: OutOfMemoryError => println(" throws OutOfMemoryError") 
      case t: Throwable => println(" throws some other exception" + t)
    }
  }
  
  def main(args: Array[String]): Unit = {
    
    def getData: EphemeralStream[Int] = ones
    
    def processData(s: EphemeralStream[Int]) = {}

    getData match {
      case x ##:: xs => processData(x ##:: xs)
      case _ => println("no match")
    }

    getData match {
//      case s @ (_ ##:: _) => processData(s)
      case _ => println("no match")
    }
    
    val foo: Option[(Int, EphemeralStream[Int])] = EphemeralStream.##::.unapply(getData)
    if (foo.isEmpty) println("No data to process")
    else {val x = foo.get._1; val xs = foo.get._2; processData(x ##:: xs)}
    
//    test("(ones drop 1000000).head"){(ones drop 1000000).head}
//    test("(ones take 1000000).length"){(ones take 1000000).length}     
    
    def plus(x: => Int)(y: => Int) = x+y
    test("(ones take 1000000).foldLeft(0)(plus)"){(ones take 1000000).foldLeft(0)(plus)}     
//    test("(ones take 1000000).reduceLeft(_ + _)"){(ones take 1000000).reduceLeft(_ + _)}     
    test("{var sum = 0; (ones take 1000000).foreach(x => sum += x); sum}"){
      var sum = 0; (ones take 1000000).foreach(x => sum += x); sum
    }     
    test("(ones take 1000000).sum"){(ones take 1000000).sum}
    test("(0 /: (ones take 1000000))(_ + _)"){(0 /: (ones take 1000000))(_ + _)}
    test("ones take 1000000 forall (_ == 1)"){ones take 1000000 forall (_ == 1)}
    test("ones take 1000000 exists (_ != 1)"){ones take 1000000 exists (_ != 1)}
    test("ones take 1000000 find (_ != 1)"){ones take 1000000 find (_ != 1)}
    test("sum(ones take 1000000)"){sum(ones take 1000000)}
    test("sumPatMat(ones take 1000000)"){sumPatMat(ones take 1000000)}
    test("sumByName(ones take 1000000)"){sumByName(ones take 1000000)}  
    test("sumTailRec(ones take 1000000)"){sumTailRec(ones take 1000000)}  
    test("sumImperative(ones take 1000000)"){sumImperative(ones take 1000000)}  
    test("leaky(ones take 1000000)"){leaky(ones take 1000000)}  
    test("traitSumTailRec(ones take 1000000)"){traitSumTailRec(ones take 1000000)}
    test("traitSumImperative(ones take 1000000)"){traitSumImperative(ones take 1000000)}
    test("traitSumByName(ones take 1000000)"){traitSumByName(ones take 1000000)}
  }
}

