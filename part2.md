**TL;DR: The collective wisdom is wrong.**

In Part I, we discussed the rules for ensuring that Scala `Stream`s do not 
leak memory. 

1.  Define streams using `def` and never store them in `val`s.

2.  Pass streams around via by-name parameters, consume them in tail-recursive functions.

3.  Corollary: When defining stream-consuming functions in traits,
    wrap them in methods accepting streams as by-name parameters.

4.  Do not pattern match against streams outside the tail-recursive consuming functions.

5.  Only call the eagerly evaluated `Stream` methods that are marked as *"optimized for GC"*.

It turns out, however, that **rules 2 to 5 are superfluous if your program
will always run on a JVM equipped with a precise garbage collector.**

Consider rule #2:

2.  **Pass streams around via by-name parameters, consume them in tail-recursive functions.**

We did not discuss in Part I how tail-recursive functions are different.
Let's decomplie an example:

      @tailrec
      def sum(xs: Stream[Int], z: Int = 0): Int = 
        if (xs.isEmpty) z else sum(xs.tail, z + xs.head)

Here is what the decompiler produces:

      public int sum(Stream<Object> xs, int z) {
        for (;;) {
          if (xs.isEmpty()) return z;
          z += BoxesRunTime.unboxToInt(xs.head());
          xs = (Stream)xs.tail();
        }
      }

Notice that the `xs` parameter is reused. It gets overwritten on each loop 
iteration, so it always holds a reference to the not-yet processed 
*remainder* of the original stream.

However, it turns out that it was perfectly possible to consume a stream 
in an imperative loop in the first place. But in Scala, function parameters 
are essentially `val`s and hence cannot be reused in such a manner.
So a local `var` has to be introduced:

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
*is not used* after the initial assigment to `scan` and therefore 
does not consider it being a GC root after that assignment.

Wait a minute. 

Here is the first example from part I again:

      def sum(xs: Stream[Int]): Int = {
        @tailrec
        def loop(acc: Int, xs: Stream[Int]): Int =
          if (xs.isEmpty) acc else loop(acc+xs.head, xs.tail)
        loop(0, xs)
      }

In it, the `xs` parameter of `sum` is also not used after the call of `loop`. 
Why does the JVM think otherwise?
How is this different from the imperative implementation of `sum` above? 

Let's do a small experiment. The first snippet below throws an OOM error, 
just as anyone who've read Part I would have expected:

      def ones: Stream.continually(1)
      println(sum(ones take 1000000000)) // OOM

but the second one works:

      def ones: Stream.continually(1)
      for (i <- 1 to 10000) {
        sum(ones take 10)
      }
      println(sum(ones take 1000000000)) // Works!

What's going on here?

The difference is that the imperative version of `sum` gets JIT-compiled 
after a certain number or loop iterations! It is the HotSpot compiler that
is capable of calculating the *life time* of `sum` parameter.

The second version of `sum` also stopped leaking memory after 
having been applied enough times to trigger its own JIT compilation.
                    
And of course, it would not have leaked memory at all if JIT compilation 
was forced using the HotSpot `-Xcomp` option, or if the JVM had a precise GC 
and no interpreter at all.

In fact, **all "faulty" tests from Part I pass on HotSpot with `-Xcomp`.**

Which means that defining stream-consuming functions in traits 
makes no difference after the forwarders get JIT compiled.

And also that the non-specialized `TraversableOnce` methods 
work after JIT compilation.

As far as pattern matching is concerned, you still have to make sure
that pattern variables are not used after the call 
of a stream-consuming function. As you saw in Part I, those
variables are implicit `val`s, and Rule #1 holds.

For instance, the following function 
leaks memory regardless of whether `-Xcomp` is present:

      def tailAverage(xs: Stream[Int]): Option[Double] = {
        xs match {
          case Stream.Empty => None
          case y #:: Stream.Empty => None
          case y #:: ys => Some(sum(ys) / ys.length)
        }
      }

What if you want your code to be JVM-agnostic? You can try sticking
to Rules #2-5, and of course Rule #1.