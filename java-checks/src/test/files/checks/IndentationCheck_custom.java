import java.util.stream.IntStream;

class Foo {
  int a;                          // Noncompliant {{Make this line start after 4 spaces to indent the code consistently.}}
   int b;                         // Compliant - already reported
 int c;                           // Compliant - already reported

  public void foo1() {            // Compliant
    System.out.println();         // Noncompliant {{Make this line start after 8 spaces to indent the code consistently.}}
    }                             // Compliant

 public void foo2() {             // Compliant
   System.out.println("hehe");    // Noncompliant
    System.out.println();         // Compliant - already reported
  }

  public void foo3() {            // Compliant
System.out.println();             // Noncompliant
System.out.println();             // Compliant - already reported
System.out.println();             // Compliant

if (true) {                       // Compliant
  System.out.println();           // Noncompliant {{Make this line start after 12 spaces to indent the code consistently.}}
  if (true) {                     // Compliant
        System.out.println();     // Noncompliant {{Make this line start after 16 spaces to indent the code consistently.}}
    System.out.println();         // Compliant
  }

      ; System.out.println();     // Compliant
}
}

  class Foo {                     // Noncompliant {{Make this line start after 4 spaces to indent the code consistently.}}

        int a;                    // Compliant

  int b;                          // Noncompliant {{Make this line start after 8 spaces to indent the code consistently.}}

  }
}

enum Bar {
  A,
 B,
   C;

  public void foo1() {            // Noncompliant {{Make this line start after 4 spaces to indent the code consistently.}}
  }

 public void foo2() {             // Compliant
 }
}

interface Qix {

 void foo1();                     // Noncompliant

  void foo2();                    // Compliant

}

class Baz {

  void foo() {                    // Noncompliant {{Make this line start after 4 spaces to indent the code consistently.}}
    new MyInterface() {           // Noncompliant {{Make this line start after 8 spaces to indent the code consistently.}}
        public void foo() {       // Compliant
        }
          public void bar() {     // Noncompliant
          }
    };
  }

  int[] foo = new int[] {
    0,
    new Foo()
  };

}

 class Qiz {                             // Noncompliant
  public void foo() {                    // Noncompliant
    switch (0) {                         // Noncompliant
      case 0:
        System.out.println(); System.out.println(); // Noncompliant
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
        break;   // Noncompliant
    }

    switch (foo) {
      case 1: break; // Noncompliant
      case 2
        : case 3: break; // Compliant
    }
  };
  static {
    try{ // Noncompliant {{Make this line start after 8 spaces to indent the code consistently.}}
       while (keys.hasMoreElements()) { // Noncompliant {{Make this line start after 12 spaces to indent the code consistently.}}
        s = keys.nextElement();         // Noncompliant {{Make this line start after 16 spaces to indent the code consistently.}}
        rId = (String) s;
        cName = (String) exceptionClassNames.get(rId);
        exceptionRepositoryIds.put (cName, rId);
      }
    } catch (NoSuchElementException e) { }
  }
}
@interface Example {
  public static class Inner {                    // Noncompliant {{Make this line start after 4 spaces to indent the code consistently.}}
    public static final String FOO = "foo";      // Noncompliant {{Make this line start after 8 spaces to indent the code consistently.}}
  }
}

class Lambda {
    void foo() {
        IntStream
            .range(1, 5)
            .map((a -> {
                return a + 1;
            }));
        IntStream
            .range(1, 5)
            .map((a -> {
              return a + 1; // Noncompliant {{Make this line start after 16 spaces to indent the code consistently.}}
            }));

        IntStream
            .range(1, 5)
            .map((
                a -> {
                    return a + 1; // Compliant
                }));
    }
}
