/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package streamhygiene

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

//    println((fib take 1000000000).traitSum)  // Works
//    println(myFSum(0, myFib take 1000000000))  // Works with def tail
//    println(myTest)  // fails
//    println(test)  // fails
//    println(fsum(0, fib take 1000000000))  // OOM
//    def ones1: MyStream = new MyCons(1, ones1)
//    println((ones1 traitTake 1000000000).traitSum)  // Works!
//    println((ones1 take 1000000000).sum)  // Works
//    println(sumImperative(ones take 1000000000))  // Works
