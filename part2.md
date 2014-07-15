[The source code for this post is available on GitHub.](https://github.com/dmitryleskov/stream-hygiene/tree/master/src/streamhygiene/part2)

**TL;DR: The collective wisdom can be wrong.**

In Part I, we discussed the rules for ensuring that Scala `Stream`s do not 
leak memory:

1.  Define streams using `def` and never store them in `val`s.

2.  Consume streams in tail-recursive functions.

3.  Pass streams around via by-name parameters.

    Corollary: When defining stream-consuming functions in traits,
    wrap them in methods accepting streams as by-name parameters.

4.  Do not pattern match against streams outside the tail-recursive consuming functions.

5.  Only call the eagerly evaluated `Stream` methods that are marked as *"optimized for GC"*.

It turns out, however, that **rules 2 to 5 are superfluous in the 
presence of a precise garbage collector.**

Consider rule #2:

2.  **Consume streams in tail-recursive functions.**

Let's decompile the example from Part I again:

      @tailrec
      def sum(xs: Stream[Int], z: Int = 0): Int = 
        if (xs.isEmpty) z else sum(xs.tail, z + xs.head)

Decompiler output:

      public int sum(Stream<Object> xs, int z) {
        for (;;) {
          if (xs.isEmpty()) return z;
          z += BoxesRunTime.unboxToInt(xs.head());
          xs = (Stream)xs.tail();
        }
      }

The `xs` parameter gets overwritten at the end of each loop iteration
with the *remainder* of the original stream.

However, it turns out that it was perfectly possible to consume a stream 
in an imperative loop in the first place. Remember that, unlike in Java,
in Scala function parameters are essentially `val`s and hence cannot be 
reused. So a local `var` has to be introduced:

      def sum(xs: Stream[Int]): Int = {
        var scan = xs
        var res = 0
        while (!scan.isEmpty) {
          res += scan.head
          scan = scan.tail
        }
        res
      }

The `xs` parameter is not changed and therefore should hold a reference 
to the original stream, but, somehow, it does not?!

What happens here is that the JVM detects that the `xs` parameter
*is not used after the initial assigment to `scan`* and therefore 
does not consider it being a GC root after that assignment.

Wait a minute. 

Here is the example from Rule #3:

      def sum(xs: Stream[Int]): Int = {
        @tailrec
        def loop(acc: Int, xs: Stream[Int]): Int =
          if (xs.isEmpty) acc else loop(acc+xs.head, xs.tail)
        loop(0, xs)
      }

Here, the `xs` parameter of `sum` is also not used after the call of `loop`. 
Why does the JVM think otherwise?
How is this different from the imperative implementation of `sum` above? 

Let's do a small experiment in the REPL:

<pre><code><span style="color: #888">scala></span> def ones = Stream.continually(1)
ones: scala.collection.immutable.Stream[Int]

<span style="color: #888">scala></span> println((ones take 100000000).sum)
java.lang.OutOfMemoryError: Java heap space
        at scala.collection.immutable.Stream$.continually(Stream.scala:1129)
   .  .  .
        at scala.collection.immutable.Stream.foldLeft(Stream.scala:563)
        at scala.collection.TraversableOnce$class.sum(TraversableOnce.scala:203)
   .  .  .

<span style="color: #888">scala></span> for (i <- 1 to 10000) {
<span style="color: #888">     |</span>   (ones take 10).sum
<span style="color: #888">     |</span> }

<span style="color: #888">scala></span> println((ones take 100000000).sum)
100000000

<span style="color: #888">scala></span>
</code></pre>

The first call to `(ones take 100000000).sum` threw an OOM error, 
just as anyone who've read Part I would have expected, but the second 
one magically worked!

What's going on here?

As you may see, `Stream` mixes in the `sum` implementation from the 
`TraversableOnce` trait, where it is defined as:

      def sum[B >: A](implicit num: Numeric[B]): B = foldLeft(num.zero)(num.plus)

The difference is that `sum` got JIT-compiled in between of the two `println`
calls! 
It is the HotSpot compiler that is capable of calculating the *life time* of 
variables and parameters. In this particular case, it determines that the 
receiver is not used after the `foldLeft` call.

The imperative version of `sum` contains a loop, so after some iterations
the JVM considered it a "hot spot" and the JIT compiler kicked in.
But even if a function itself does not contain a loop, *applying* it many times 
also triggers its JIT compilation.
In the REPL session shown above, `sum` gets applied to ten-element streams
10,000 times, which happens to be the default threshold for the HotSpot Server VM 
(for the client it is just 1,500). 

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
variables are implicit `val`s, and Rule #1 holds the sophisicatedness
of the underlying JVM notwithstanding.

For instance, the following function 
leaks memory regardless of whether `-Xcomp` is present:

      def tailAvg(xs: Stream[Int]): Option[Int] = {
        xs match {
          case Stream.Empty => None
          case y #:: Stream.Empty => None
          case y #:: ys => Some(ys.sum / ys.length)
        }
      }

**But what if you want your code to be JVM-agnostic, without 
having to stick to The Rules?**

There are two options, considered in Part III.
