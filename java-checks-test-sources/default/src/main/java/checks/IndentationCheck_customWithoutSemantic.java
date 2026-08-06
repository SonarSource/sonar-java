package checks;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

class FooIdentationWS {
   int b;                         // Noncompliant
 int c;                           // Compliant - already reported

  public void foo1() {            // Compliant
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
    System.out.println();         // Noncompliant
  }

      ; System.out.println();     // Compliant
}
}

        int a;
}

enum BarIdentationWS {
  A,
 B,
   C;

 public void foo2() {             // Noncompliant
 }
}

interface QixIdentationWS {

 void foo1(); // Noncompliant

  void foo2();                    // Compliant

}

class BazIdentationWS {

  Object[] foo = new Object[] {
    0,
    new FooIdentationWS()
  };

}

 class QizIndentationWS { // Noncompliant
  static List<Integer> list = List.of(1,2,3);
  static {
       while (list.isEmpty()) { // Noncompliant
        String k = "hello"; // Noncompliant
      }
  }
}
@interface ExampleWS {
}

class LambdaIndentationWS {
    void foo() { // Noncompliant
        IntStream // Noncompliant
            .range(1, 5)
            .map((a -> {
                return a + 1; // Noncompliant
            }));
        IntStream
            .range(1, 5)
            .map((a -> {
              return a;
            }));

        IntStream
            .range(1, 5)
            .map((
                a -> {
                    return a + 1; // Noncompliant
                }));
    }
}
