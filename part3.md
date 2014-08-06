# Life Without Rules #
[The source code for this post is available on GitHub.](https://github.com/dmitryleskov/stream-hygiene/tree/master/src/streamhygiene/part3)

The following is the final part in a three-part series.
[Part I](http://blog.dmitryleskov.com/programming/scala/stream-hygiene-i-avoiding-memory-leaks/)
listed the coding rules that help you avoid memory leaks when using the standard
Scala `Stream` class.
[Part II](http://blog.dmitryleskov.com/programming/scala/stream-hygiene-ii-hotspot-kicks-in/)
demonstrated that an optimizing JIT compiler and precise GC render most of those rules superfluous,
and argued that circumventing `Stream` memoization in production code is dangerous.
This part will discuss the alternatives to `Stream` for representing
potentially infinite data structures without the risk of leaking memory.

## scalaz.EphemeralStream ##

[Scalaz](https://github.com/scalaz/scalaz) 
is an open-source Scala library that implements type classes and
pure functional data structures. It contains a class (a trait, to be more precise)
`EphemeralStream`, described as follows:

> Like `scala.collection.immutable.Stream`, but doesn't save computed values. 
As such, it can be used to represent similar things, but without the space 
leak problem frequently encountered using that type.

The "does not save computed values" is misleading.
An `EphemeralStream` cell actually caches both the value and the 
next cell reference (if it has been computed) using Java weak references.

Because objects that only have weak references to them get garbage collected,
you can safely store an `EphemeralStream` in a `val` and pattern match on it 
as you please:

~~~ {.scala}
def tailAvg(xs: EphemeralStream[Int]): Option[Int] = {
  xs match {
    case y ##:: ys => Some(ys.sum / ys.length)
    case _ => None
  }
}
~~~

Unfortunately, `EphemeralStream` has numerous issues hindering its practical use:

 -  It implements memoization using Java `WeakReference`s wrapped in Scala
    `Option`s wrapped in closures, which all add memory overheads, so the GC gets 
    invoked more frequently compared to the technique described in Part I.

 -  Because of all that wrapping, it is also way slower than standard `Stream` 
    even if each element is only accessed once. The `EphemeralStream.length` method,
    which does not access stream elements, is about five times slower
    than `Stream.length`. Computing sum of a stream of `Int`s takes approximately
    3x more time.

 -  It is poorly documented and comes with no usage examples. 
    In fact, the *only* comment in its source is the (misleading) 
    scaladoc comment quoted above in its entirety.
    Looks like an auxiliary class to me.

 -  It does not implement `Stream` convenience methods such as `from` or
    `continually`. That said, the scarcity of the method list in `EphemeralStream` 
    documentation is once again misleading: an `EphemeralStream` can be implicitly 
    converted to an `Iterable`, so all `Iterable` methods are in fact 
    available.
     
 -  The `scalaz-core` jar is 9MB in size, which is a bit too big an overhead
    if you only need a single class.

There is also a couple of minor inconveniences: 

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
 
 -  There is no empty `EphemeralStream[Nothing]` object, so you cannot 
    match against a pattern similar to `x #:: Stream.Empty`. Workaround: 
    
    ~~~ {.scala}
    case x ##:: xs if xs.isEmpty => ...
    ~~~

## Roll out your own ##

Use [scalaz.EphemeralStream](http://scalaz-seven-doc.cleverapps.io/core/target/scala-2.10/api/index.html#scalaz.EphemeralStream),
or roll out your own non-memoizing alternative to standard Scala streams.

