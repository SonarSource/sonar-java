import java.util.stream.IntStream;

class Foo {
  int a;                          // Noncompliant {{Make this line start after 4 spaces instead of 2 in order to indent the code consistently. (Indentation level is at 4.)}}
   int b;                         // Compliant - already reported
 int c;                           // Compliant - already reported

  public void foo1() {            // Compliant
    System.out.println();         // Noncompliant {{Make this line start after 8 spaces instead of 4 in order to indent the code consistently. (Indentation level is at 4.)}}
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
  System.out.println();           // Noncompliant {{Make this line start after 12 spaces instead of 2 in order to indent the code consistently. (Indentation level is at 4.)}}
  if (true) {                     // Compliant
        System.out.println();     // Noncompliant {{Make this line start after 16 spaces instead of 8 in order to indent the code consistently. (Indentation level is at 4.)}}
    System.out.println();         // Compliant
  }

      ; System.out.println();     // Compliant
}
}

  class Foo {                     // Noncompliant {{Make this line start after 4 spaces instead of 2 in order to indent the code consistently. (Indentation level is at 4.)}}

        int a;                    // Compliant

  int b;                          // Noncompliant {{Make this line start after 8 spaces instead of 2 in order to indent the code consistently. (Indentation level is at 4.)}}

  }
}

enum Bar {
  A,
 B,
   C;

  public void foo1() {            // Noncompliant {{Make this line start after 4 spaces instead of 2 in order to indent the code consistently. (Indentation level is at 4.)}}
  }

 public void foo2() {             // Compliant
 }
}

interface Qix {

 void foo1();                     // Noncompliant

  void foo2();                    // Compliant

}

class Baz {

  void foo() {                    // Noncompliant {{Make this line start after 4 spaces instead of 2 in order to indent the code consistently. (Indentation level is at 4.)}}
    new MyInterface() {           // Noncompliant {{Make this line start after 8 spaces instead of 4 in order to indent the code consistently. (Indentation level is at 4.)}}
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
    try{ // Noncompliant {{Make this line start after 8 spaces instead of 4 in order to indent the code consistently. (Indentation level is at 4.)}}
       while (keys.hasMoreElements()) { // Noncompliant {{Make this line start after 12 spaces instead of 7 in order to indent the code consistently. (Indentation level is at 4.)}}
        s = keys.nextElement();         // Noncompliant {{Make this line start after 16 spaces instead of 8 in order to indent the code consistently. (Indentation level is at 4.)}}
        rId = (String) s;
        cName = (String) exceptionClassNames.get(rId);
        exceptionRepositoryIds.put (cName, rId);
      }
    } catch (NoSuchElementException e) { }
  }
}
@interface Example {
  public static class Inner {                    // Noncompliant {{Make this line start after 4 spaces instead of 2 in order to indent the code consistently. (Indentation level is at 4.)}}
    public static final String FOO = "foo";      // Noncompliant {{Make this line start after 8 spaces instead of 4 in order to indent the code consistently. (Indentation level is at 4.)}}
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
              return a + 1; // Noncompliant {{Make this line start after 16 spaces instead of 14 in order to indent the code consistently. (Indentation level is at 4.)}}
            }));

        IntStream
            .range(1, 5)
            .map((
                a -> {
                    return a + 1; // Compliant
                }));
    }
}
