/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package streamhygiene

abstract class MyStream[+A] {
  def isEmpty: Boolean
  def head: A
  def tail: MyStream[A]

  def take(n: Int): MyStream[A] =
    if (n == 0 || isEmpty) MyEmptyStream else new MyCons(this.head, this.tail.take(n-1))
  
  def foldLeft[B](z: B)(f: (A, B) => B): B = {
    var acc = z
    var scan = this
    while (!scan.isEmpty) {
      acc = f(scan.head, acc)
      scan = scan.tail
    }
    acc
  }

}

class MyCons[+A](h: A, t: => MyStream[A]) extends MyStream[A] {
  def isEmpty = false
  def head = h
  def tail = t
}

object MyEmptyStream extends MyStream[Nothing] {
  def isEmpty = true
  def head = throw new Error("Head of empty MyStream")
  def tail = throw new Error("Tail of empty MyStream")
}
