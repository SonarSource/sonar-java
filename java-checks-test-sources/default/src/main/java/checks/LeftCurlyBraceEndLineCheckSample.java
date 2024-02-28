package checks;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Documented;
import java.util.List;
import org.sonar.api.Properties;

class LeftCurlyBraceEndLineCheckSample {
  
  private static final int MY_CONST;
  static
  {                                    // Noncompliant {{Move this left curly brace to the end of previous line of code.}} [[sc=3;ec=4]]
    MY_CONST = 0;
  }
  
  public enum MyEnum {
    A(1)
    // Duplicated issue...
    // Noncompliant@+1
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
    },
    B(2),
    C(42) {                             // Compliant
    };
    
    int value;
    
    MyEnum(int value) {
      this.value = value;
    }
  }
  
  public LeftCurlyBraceEndLineCheckSample() {                       // Compliant
  }
  
  public void bar() throws Exception
  {                                     // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
    class InnerClass
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
    }
  }
  
  void doStuff(boolean test, MyEnum myEnum, List myList) throws IOException
  {                                     // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
    if (test)
      System.out.println();
    
    if (test) {                         // Compliant
    } else
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
    }
    
    switch (myEnum)
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
      case A:
        break;
      default:
        break;
    }
    
    do {                                // Compliant
    } while(test);
    
    while (test)
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
    }
    
    for (int i = 0; i < 10; i++)
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
    }
    
    for (Object object : myList) {      // Compliant
    }
    
    synchronized (myList)
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
    }
    
    try
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
    } catch(Exception e)
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
    }
    
    try (FileInputStream fis = new FileInputStream(""))
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
    } finally
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
    }
    
    Closeable c = new Closeable()
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
      @Override
      public void close() throws IOException {
      }
    };
    
    Closeable c2 = new Closeable() {    // Compliant
      @Override
      public void close() throws IOException {
        
      }
    };
    
    LABEL:
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
      doSomethingElse();
    }
  }
  
  public void myMethod(boolean something, boolean something3, boolean something4, boolean param1, boolean param2, boolean param3) {              // Compliant
    if(something)
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
      doSomethingElse();
    } else {                            // Compliant
      doSomethingElse();
    }
    if( param1 && param2 && param3
      && something3 && something4)
    {                                   // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
      doSomethingElse();
    }
  }

  private void doSomethingElse() {
  }

  {                                     // Compliant
    System.out.println("static intializer");
  }

  public void foo() {
    {                                   // Compliant
    }
  }
}

class LeftCurlyBraceEndLineCheckSampleBar extends LeftCurlyBraceEndLineCheckSample {                 // Compliant
}

class LeftCurlyBraceEndLineCheckSampleReBar extends checks.LeftCurlyBraceEndLineCheckSample {         // Compliant
}

abstract class Dul implements Closeable
{                                       // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
}

class Goo<T>
{                                       // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
}

abstract class MyList<E> implements List<E>
{                                       // Noncompliant {{Move this left curly brace to the end of previous line of code.}}
}

@Properties(
{                                       // Compliant
})
class Exceptions {
  int[] numbers = new int[]
{ 0, 1 };                               // Compliant
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
@Documented
@interface ConstructorProperties {
 /**
    <p>The getter names.</p>
    @return the getter names corresponding to the parameters in the
    annotated constructor.
 */
 String[] value();
}
