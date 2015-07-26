class Foo {
  int a;                          // Compliant
   int b;                         // Noncompliant {{Make this line start at column 3.}}
 int c;                           // Compliant - already reported

  public void foo1() {            // Compliant
    System.out.println();         // Compliant
    }                             // Compliant

 public void foo2() {             // Compliant
   System.out.println("hehe");    // Noncompliant
    System.out.println();         // Compliant - already reported
  }

  public void foo3() {            // Compliant
System.out.println();             // Noncompliant
System.out.println();             // Compliant - already reported
System.out.println();             // Compliant - already reported

if (0) {                          // Compliant - already reported
  System.out.println();           // Noncompliant
  if (0) {                        // Compliant - already reported
        System.out.println();     // Compliant
    System.out.println();         // Noncompliant {{Make this line start at column 9.}}
  }

      ; System.out.println();     // Compliant
}
}

  class Foo {

    int a;                        // Compliant

  int b;                          // Noncompliant

  }
}

enum Bar {
  A,
 B,
   C;

  public void foo1() {            // Compliant
  }

 public void foo2() {             // Noncompliant
 }
}

interface Qix {

 void foo1();                     // Noncompliant

  void foo2();                    // Compliant

}

static class Baz {

  void foo() {
    new MyInterface() {
      public void foo() {         // Compliant - not checked
      }

     public void foo() {          // Compliant - not checked
     }
    };
  }

  int[] foo = new int[] {
    0,
    new Foo()
  };

}

 static class Qiz {                      // Noncompliant
  public void foo() {
    switch (0) {
      case 0:
        System.out.println(); System.out.println(); // Compliant
        break;
    }

    System.out.println( // Compliant
        ); Sysout.out.println(); // Compliant

    switch (foo) { // Compliant
    }

    switch (foo) { // Compliant
      case 0:
      case 1:
      case 2:
      case 3:
        break;
    }

    switch (foo) {
      case 1: break; // Noncompliant
      case 2
        : case 3: break; // Compliant
    }
  };
  static {
    try{
       while (keys.hasMoreElements()) { // Noncompliant {{Make this line start at column 7.}}
        s = keys.nextElement();
        rId = (String) s;
        cName = (String) exceptionClassNames.get(rId);
        exceptionRepositoryIds.put (cName, rId);
      }
    } catch (NoSuchElementException e) { }
  }
}
public @interface Example {
  public static class Inner {
    public static final String FOO = "foo";
  }
}
