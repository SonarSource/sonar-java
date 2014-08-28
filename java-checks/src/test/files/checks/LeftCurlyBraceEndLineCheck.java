class Foo {
  public void myMethod() {              // Compliant
    if(something)
    {                                   // Non-Compliant
      executeTask();
    } else {                            // Compliant
      doSomethingElse();
    }
    if( param1 && param2 && param3
      && something3 && something4)
    {                                   // Non-Compliant
      executeAnotherTask();
    }
  }

  {                                     // Compliant
    System.out.println("static intializer");
  }

  public void foo() {
    {                                   // Compliant
    }
  }
}

@Properties(
{ // Compliant
})
class Exceptions {
  int[] numbers = new int[]
{ 0, 1 }; // Compliant
}

/**
<p>An annotation on a constructor that shows how the parameters of
that constructor correspond to the constructed object's getter
methods.  For example:

<blockquote>
<pre>
public class Point {
    &#64;ConstructorProperties({"x", "y"})
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    private final int x, y;
}
</pre>
</blockquote>

The annotation shows that the first parameter of the constructor
can be retrieved with the {@code getX()} method and the second with
the {@code getY()} method.  Since parameter names are not in
general available at runtime, without the annotation there would be
no way to know whether the parameters correspond to {@code getX()}
and {@code getY()} or the other way around.</p>

@since 1.6
*/
@Documented @Target(CONSTRUCTOR) @Retention(RUNTIME)
public @interface ConstructorProperties {
 /**
    <p>The getter names.</p>
    @return the getter names corresponding to the parameters in the
    annotated constructor.
 */
 String[] value();
}
