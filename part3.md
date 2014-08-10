[The source code for this post is available on GitHub.](https://github.com/dmitryleskov/stream-hygiene/tree/master/src/streamhygiene/part3)

The following is the third part in a four-part series.
[Part I](http://blog.dmitryleskov.com/programming/scala/stream-hygiene-i-avoiding-memory-leaks/)
listed the coding rules that help you avoid memory leaks when using the standard
Scala `Stream` class.
[Part II](http://blog.dmitryleskov.com/programming/scala/stream-hygiene-ii-hotspot-kicks-in/)
demonstrated that an optimizing JIT compiler and precise GC render most of those rules superfluous,
and argued that circumventing `Stream` memoization in production code is dangerous.
This part will discuss a readily available third-party alternative 
that enables representing potentially infinite data structures 
without the risk of leaking memory:

## scalaz.EphemeralStream ##

[Scalaz](https://github.com/scalaz/scalaz) 
is an open-source Scala library that implements type classes and
pure functional data structures. It contains a class (a trait, to be more precise)
`EphemeralStream`, described as follows:

> Like `scala.collection.immutable.Stream`, but doesn't save computed values. 
As such, it can be used to represent similar things, but without the space 
leak problem frequently encountered using that type.

Don't let the scarcity of the method list in `EphemeralStream` documentation 
mislead you: an `EphemeralStream` can be implicitly 
converted to an `Iterable`, so *most* methods of the latter are in fact 
available (but not *all* of them, more on that below).

The "does not save computed values" part is somewhat misleading too.
An `EphemeralStream` cell actually caches both the value and the 
next cell reference (once it gets computed) using Java weak references.

Objects that only have weak references to them get garbage collected 
on first try. Therefore you can safely store an `EphemeralStream` in a `val` and 
pattern match on it as you please:

~~~ {.scala}
def tailAvg(xs: EphemeralStream[Int]): Option[Int] = {
  xs match {
    case y ##:: ys => Some(ys.sum / ys.length)
    case _ => None
  }
}
~~~

Unfortunately, `EphemeralStream` suffers from numerous issues 
hindering its practical use:

 -  It implements memoization using Java `WeakReference`s wrapped in Scala
    `Option`s wrapped in closures, which all add memory overheads, so the GC gets 
    invoked more frequently compared to the technique described in Part I.
    For instance, an `EphemeralStream[Int]` needs exactly twice as much memory 
    as a `Stream[Int]` on the 32-bit Java HotSpot Server VM. 

 -  Because of all that wrapping, it is also way slower than a standard `Stream` 
    even if each element is only accessed once. Depending on the number of elements,
    the `EphemeralStream.length` method, which does not access elements at all, 
    was *from three to six times slower* than `Stream.length` in my tests.
    Computing sum of an `EphemeralStream[Int]` took approximately 3x more time
    compared to a `Stream[Int]`.

 -  It is poorly documented and comes with no usage examples. 
    In fact, the *only* comment in its source is the (misleading) 
    scaladoc comment quoted above in its entirety.
    Looks like an auxiliary class to me.

 -  The `scalaz-core` jar is 9MB in size, which is a bit too big an overhead
    if you only need a single class. And it is not easy to extract 
    `EphemeralStream` for standalone use, because it depends on other `Scalaz` 
    classes and traits.

There are also quite a few minor incompatibilities between
`scalaz.EphemeralStream` and the standard Scala `Stream` that prevent using 
the former as a drop-in replacement for the latter: 

 -  There is no empty `EphemeralStream[Nothing]` object, so you cannot 
    match against a pattern similar to `x #:: Stream.Empty`. Workaround: 
    
    ~~~ {.scala}
    case x ##:: xs if xs.isEmpty => ...
    ~~~

 -  The fold methods have different signatures, with curried functions:

    ~~~ {.scala}
    def foldLeft[B](z: => B)(f: (=> B) => (=> A) => B): B
    def foldRight[B](z: => B)(f: (=> B) => (=> A) => B): B
    ~~~

    so you cannot pass a shorthand such as `_ + _` as the second parameter
    and instead have to write:

    ~~~ {.scala}
    input.foldLeft(0)(x => y => x + y)
    ~~~
 
  - `EphemeralStream` does not implement convenience methods `from` and `continually`.
  
  - As of Scala 2.10, the `scalac` compiler seems to be unable to infer that
    an implicit conversion of an `EphemeralStream[Int]` to an `Iterable[Int]`
    yields an `Iterable[Numeric]`, and that `Ordering` is defined, 
    so methods such as `sum` and `min` are not available either.
    
If `scalaz.EphemeralStream` is not the solution, the only option left is 
to roll out our own non-leaky stream class.
In Part IV we'll try to do just that. Stay tuned!