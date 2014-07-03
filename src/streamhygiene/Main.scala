/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package streamhygiene
import scala.annotation.tailrec

trait Summator {
  def foldLeft(xs: Stream[Int], z: Int)(f: (Int, Int) => Int): Int = {
    var acc = z
    var scan: Stream[Int] = xs
    while (!scan.isEmpty) {
      acc = f(acc, scan.head)
      scan = scan.tail
    }
    acc
  }

  def sum(xs: Stream[Int]) = {
    foldLeft(xs, 0)(_ + _)
  }
  
  def foldLeftWrapped(xs: Array[Stream[Int]], z: Int)(f: (Int, Int) => Int): Int = {
    var acc = z
    var scan: Stream[Int] = xs(0)
    xs(0) = null
    while (!scan.isEmpty) {
      acc = f(acc, scan.head)
      scan = scan.tail
    }
    acc
  }
  
  def sumWrapped(xs: Array[Stream[Int]]) = {
    foldLeftWrapped(xs, 0)(_ + _)
  }
}

trait MySummer {
  this: MyStream =>
  def traitTake(n: Int): MyStream =
    if (n == 0 || isEmpty) MyEmptyStream else new MyCons(this.head, this.tail.take(n-1))
  
  def traitFoldLeft(z: Int)(f: (Int, Int) => Int): Int = {
    var acc = z
    var scan = this
    while (!scan.isEmpty) {
      acc = f(acc, scan.head)
      scan = scan.tail
    }
    acc
  }

  def traitSum = traitFoldLeft(0)(_ + _)
}

abstract class MyStream extends MySummer {
  def isEmpty: Boolean
  def head: Int
  def tail: MyStream

  def take(n: Int): MyStream =
    if (n == 0 || isEmpty) MyEmptyStream else new MyCons(this.head, this.tail.take(n-1))
  
  def foldLeft(z: Int)(f: (Int, Int) => Int): Int = {
    var acc = z
    var scan = this
    while (!scan.isEmpty) {
      acc = f(acc, scan.head)
      scan = scan.tail
    }
    acc
  }

  def sum = foldLeft(0)(_ + _)
}

class MyCons(h: Int, t: => MyStream) extends MyStream {
  def isEmpty = false
  def head = h
  lazy val tail = t
//  def tail = t
}

object MyEmptyStream extends MyStream {
  def isEmpty = true
  def head = throw new Error("Head of empty MyStream")
  def tail = throw new Error("Tail of empty MyStream")
}

trait Fibonacci {
  def myFib: MyStream = myFib(0, 1)
  private def myFib(f0: Int, f1: Int): MyStream = 
    new MyCons(f0, myFib(f1, f0+f1))
  @tailrec 
  final def myFSum(acc: Int, s: MyStream): Int = 
    if (s.isEmpty) acc else myFSum(acc + s.head, s.tail)

  def myTest = myFSum(0, myFib take 1000000000)

  def fib: Stream[Int] = fib(0, 1)
  private def fib(f0: Int, f1: Int): Stream[Int] = 
    f0 #:: fib(f1, f0+f1)

  @tailrec 
  final def fsum(acc: Int, s: Stream[Int]): Int = 
    if (s.isEmpty) acc else fsum(acc + s.head, s.tail)
  
  def test = fsum(0, fib take 100000000)
  
}

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
   
  def prefixSum(xs: => Stream[Int], n: Int): Int = {
    @tailrec
    def loop(acc: Int, xs: Stream[Int], n: Int): Int =
      if (n == 0 || xs.isEmpty) acc else loop(acc+xs.head, xs.tail, n-1)
    loop(0, xs, n)
  }

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
  
  def test(desc: String)(f: => Any): Unit = {
    print(desc)
    try {
      println(" = " + f)
    }
    catch {
      case oom: OutOfMemoryError => println(" throws OutOfMemoryError") 
    }
  }
  
  def main(args: Array[String]): Unit = {
    
    def getData: Stream[Int] = Stream.from(0)
    
    def processData(s: Stream[Int]) = {}
    
    getData match {
      case x #:: xs => processData(x #:: xs)
      case _ => println("no match")
    }

    getData match {
      case s @ (_ #:: _) => processData(s)
      case _ => println("no match")
    }
    
    val foo: Option[(Int, Stream[Int])] = Stream.#::.unapply(getData)
    if (foo.isEmpty) println("No data to process")
    else {val x = foo.get._1; val xs = foo.get._2; processData(x #:: xs)}
    
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
    test("new Summator(){}.sum(ones take 1000000)"){new Summator(){}.sum(ones take 1000000)}     // OOM
    test("new Summator(){}.sumWrapped(Array(ones take 1000000))"){new Summator(){}.sumWrapped(Array(ones take 1000000))}     // Works
    test("sum(ones take 1000000)"){sum(ones take 1000000)}  // OOM
    test("sumByName(ones take 1000000)"){sumByName(ones take 1000000)}  
    test("sumTailRec(ones take 1000000)"){sumTailRec(ones take 1000000)}  
    test("sumImperative(ones take 1000000)"){sumImperative(ones take 1000000)}  
    test("traitSumTailRec(ones take 1000000)"){traitSumTailRec(ones take 1000000)}  // OOM
    test("traitSumImperative(ones take 1000000)"){traitSumImperative(ones take 1000000)}
    test("traitSumByName(ones take 1000000)"){traitSumByName(ones take 1000000)}
//    println((fib take 1000000000).traitSum)  // Works
//    println(myFSum(0, myFib take 1000000000))  // Works with def tail
//    println(myTest)  // fails
//    println(test)  // fails
//    println(fsum(0, fib take 1000000000))  // OOM
//    def ones1: MyStream = new MyCons(1, ones1)
//    println((ones1 traitTake 1000000000).traitSum)  // Works!
//    println((ones1 take 1000000000).sum)  // Works
//    println(sumImperative(ones take 1000000000))  // Works
//    println(prefixSum(ones, 1000000000))
//    


  }
}

