[The source code for this post is available on GitHub.](https://github.com/dmitryleskov/stream-hygiene/tree/master/src/streamhygiene/part2)

[Part I](http://blog.dmitryleskov.com/programming/scala/stream-hygiene-i-avoiding-memory-leaks/)
discussed the Scala `Stream` class usage rules that help you avoid memory leaks.
I will list them below for your convenience:

1.  Define streams using `def` and never store them in `val`s.

2.  Consume streams in tail-recursive functions.

3.  Pass streams around via by-name parameters.

    Corollary: When defining stream-consuming functions in traits,
    wrap them in methods accepting streams as by-name parameters.

4.  Do not pattern match against streams outside the consuming functions.

5.  Only call the eagerly evaluated `Stream` methods that are marked as *"optimized for GC"*.

It turns out, however, that **rules 2 to 5 are superfluous in the 
presence of a precise garbage collector.**

Consider rule #2:

2.  **Consume streams in tail-recursive functions.**

Let's decompile the example from Part I again:

~~~ {.scala}
@tailrec
def sum(xs: Stream[Int], z: Int = 0): Int = 
  if (xs.isEmpty) z else sum(xs.tail, z + xs.head)
~~~

Decompiler output:

~~~ {.java}
public int sum(Stream<Object> xs, int z) {
  for (;;) {
    if (xs.isEmpty()) return z;
    z += BoxesRunTime.unboxToInt(xs.head());
    xs = (Stream)xs.tail();
  }
}
~~~

The `xs` parameter gets overwritten at the end of each loop iteration
with the *remainder* of the original stream.

However, it turns out that it was perfectly possible to consume a stream 
in an imperative loop in the first place. Remember that, unlike in Java,
in Scala function parameters are essentially `val`s and hence cannot be 
reused. So a local `var` has to be introduced:

~~~ {.scala}
def sum(xs: Stream[Int]): Int = {
  var scan = xs
  var res = 0
  while (!scan.isEmpty) {
    res += scan.head
    scan = scan.tail
  }
  res
}
~~~

The `xs` parameter cannot be changed inside the function and therefore 
should hold a reference to the original stream, but, somehow, it does not?!

What happens here is that the JVM detects that the `xs` parameter
*is not used after the initial assignment to `scan`* and therefore 
does not consider it being a GC root after that assignment.

Wait a minute. 

Here is the example from Rule #3:

~~~ {.scala}
def sum(xs: Stream[Int]): Int = {
  @tailrec
  def loop(acc: Int, xs: Stream[Int]): Int =
    if (xs.isEmpty) acc else loop(acc+xs.head, xs.tail)
  loop(0, xs)
}
~~~

Here, the `xs` parameter of `sum` is also not used after the call of `loop`. 
Why does the JVM think otherwise?
How is this different from the imperative implementation of `sum` above? 

Let's do a small experiment in the REPL:

<pre><code><span style="color: #888">scala></span> def ones = Stream.continually(1)
ones: scala.collection.immutable.Stream[Int]

<span style="color: #888">scala></span> <span style="background-color: #ff8">println((ones take 100000000).sum)</span>
java.lang.OutOfMemoryError: Java heap space
        at scala.collection.immutable.Stream$.continually(Stream.scala:1129)
   .  .  .
        at scala.collection.immutable.Stream.foldLeft(Stream.scala:563)
        at scala.collection.TraversableOnce$class.sum(TraversableOnce.scala:203)
   .  .  .

<span style="color: #888">scala></span> for (i <- 1 to 10000) {
<span style="color: #888">     |</span>   (ones take 10).sum
<span style="color: #888">     |</span> }

<span style="color: #888">scala></span> <span style="background-color: #ff8">println((ones take 100000000).sum)</span>
100000000

<span style="color: #888">scala></span>
</code></pre>

The first call to `(ones take 100000000).sum` threw an OOM error, 
just as anyone who've read the first part of this series would have expected, 
but the second one magically worked!

What's going on here?

As you may see, `Stream` mixes in the `sum` implementation from the 
`TraversableOnce` trait, where it is defined as:

~~~ {.scala}
def sum[B >: A](implicit num: Numeric[B]): B = foldLeft(num.zero)(num.plus)
~~~~

The difference is that `sum` got JIT-compiled in between of the two `println`
calls! 
It is the HotSpot compiler that is capable of calculating the *life time* of 
variables and parameters. In this particular case, it determines that `sum`'s 
receiver is not used after the `foldLeft` call.

The imperative version of `sum` contains a loop, so after some iterations
the JVM considered it a "hot spot" and the JIT compiler kicked in.
But even if a function itself does not contain a loop, *applying* it many times 
also triggers its JIT compilation.
In the REPL session shown above, `sum` gets applied to ten-element streams
10,000 times, which happens to be the default threshold for the HotSpot Server VM 
(for the Client VM it is just 1,500). 

That is the "magic" that causes `sum` to stop leaking memory.
And of course, it would not have leaked memory at all if JIT compilation 
was forced using the HotSpot `-Xcomp` option, or if it was run on a JVM 
with a precise GC and no interpreter at all.

In fact, **all "faulty" tests for Rules #2-5 pass on HotSpot with `-Xcomp`.**

Which means that defining stream-consuming functions in traits 
makes no difference if the forwarders get JIT-compiled.

And also that the non-specialized `TraversableOnce` methods 
do not actually leak memory, but it takes an optimizing compiler 
working in collaboration with a precise GC to recognize that.

As far as pattern matching is concerned, you still have to make sure
that pattern variables are not used after the call 
of a stream-consuming function. As you saw in Part I, those
variables are implicit `val`s, and Rule #1 holds the sophisticatedness
of the underlying JVM notwithstanding.

For instance, the following function 
leaks memory regardless of whether `-Xcomp` is present:

~~~ {.scala}
def tailAvg(xs: Stream[Int]): Option[Int] = {
  xs match {
    case Stream.Empty => None
    case y #:: Stream.Empty => None
    case y #:: ys => Some(ys.sum / ys.length)
  }
}
~~~

## Square Peg, Round Hole ##

As I dug through the peculiarities of Scala implementation
and observed their interference with HotSpot optimizations, 
it has grown on me that **using "infinite" Scala `Stream`s 
*in production code* is an inherently bad idea**. After all,
`Stream` is memoizing *by design*; it is *designed* to be a lazy 
equivalent of `List`, and we've been trying to circumvent the intent 
of its authors!

That said, using the standard `Stream` class to illustrate the concept 
of potentially infinite data structures *in the context of an academic 
exercise* is probably fine. All code is under your total control, 
and usually there is not that much code, so sticking to The Rules and/or 
enforcing JIT compilation is not hard. But the teachers better warn
their students against applying this particular knowledge in production,
because:

 -  You would normally want your production code to be JVM-agnostic,
    especially if you are creating a library or framework that 
    other people will use in arbitrary contexts and environments.

 -  Without tool support, enforcing any sophisticated coding rules 
    throughout the lifetime of a project larger than a student assignment 
    is next to impossible.

 -  The authors of third-party libraries and legacy code are likely 
    to be unaware of these rules.

<small>For instance, consider the following scenario: suppose your code, 
or a third-party library you are using, breaks one of the "optional" 
rules, but all your load tests trigger JIT compilation 
of the respective classes, one way or the other.
Effectively, you will be shipping an app with a latent memory 
leak, isolating which may be quite tricky.</small>

So, if `Stream` is not the solution, what are the alternatives?

There are two options that I am aware of, and I will consider them 
in Part III. Stay tuned!
