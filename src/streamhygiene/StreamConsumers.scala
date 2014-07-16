/*
 * Originally (c) 2014 Dmitry Leskov, http://www.dmitryleskov.com
 * Released into the public domain under the Unlicense, http://unlicense.org
 */

package streamhygiene
import scala.annotation.tailrec

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
