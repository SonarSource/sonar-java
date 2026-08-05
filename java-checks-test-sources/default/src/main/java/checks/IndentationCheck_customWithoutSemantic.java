package checks;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

class FooIdentation {
  int a; // FN
   int b;                         // Noncompliant
 int c;                           // Compliant - already reported

  public void foo1() {            // Compliant
    System.out.println(); // FN
    }                             // Compliant

 public void foo2() {             // Compliant
   System.out.println("hehe"); // Noncompliant
    System.out.println();         // Compliant - already reported
  }

  public void foo3() {            // Compliant
System.out.println(); // Noncompliant
System.out.println();             // Compliant - already reported
System.out.println();             // Compliant

if (true) {                       // Compliant
  System.out.println(); // Noncompliant
  if (true) {                     // Compliant
        System.out.println(); // FN
    System.out.println();         // Noncompliant
  }

      ; System.out.println();     // Compliant
}
}

  class Foo { // FN

        int a;                    // Noncompliant

  int b; // FN

  }
}

enum BarIdentation {
  A,
 B,
   C;

  public void foo1() { // FN
  }

 public void foo2() {             // Noncompliant
 }
}

interface QixIdentation {

 void foo1(); // Noncompliant

  void foo2();                    // Compliant

}

class BazIdentation {

  void foo() { // FN
    new QixIdentation() { // FN
        public void foo1() {       // Noncompliant
        }
          public void foo2() { // FN
          }
    };
  }

  Object[] foo = new Object[] {
    0,
    new FooIdentation()
  };

}

 class QizIndentation { // Noncompliant
  public void foo(int foo) { // FN
    switch (0) { // FN
      case 0:
        System.out.println(); System.out.println(); // FN
        break;
    }

    System.out.println( // Compliant
        ); System.out.println(); // Compliant

    switch (foo) { // Compliant
    }

    switch (foo) { // Compliant
      case 0:
      case 1:
      case 2:
      case 3:
        break; // FN
    }

    switch (foo) {
      case 1: break; // Noncompliant
      case 2
        : case 3: break; // Compliant
    }
  };
  static List<Integer> list = List.of(1,2,3);
  static {
    try{ // FN
       while (list.isEmpty()) { // Noncompliant
        int s = list.get(0); // FN
        String k = "hello";
      }
    } catch (NoSuchElementException e) { }
  }
}
@interface Example {
  public static class Inner { // FN
    public static final String FOO = "foo"; // FN
  }
}

class LambdaIndentation {
    void foo() { // Noncompliant
        IntStream // Noncompliant
            .range(1, 5)
            .map((a -> {
                return a + 1; // Noncompliant
            }));
        IntStream
            .range(1, 5)
            .map((a -> {
              return a + 1; // FN
            }));

        IntStream
            .range(1, 5)
            .map((
                a -> {
                    return a + 1; // Noncompliant
                }));
    }
}
