class Foo {
  int a;                          // Compliant
   int b;                         // Non-Compliant
 int c;                           // Compliant - already reported

  public void foo1() {            // Compliant
    System.out.println();         // Compliant
    }                             // Compliant

 public void foo2() {             // Non-Compliant
   System.out.println("hehe");    // Non-Compliant
    System.out.println();         // Compliant - already reported
  }

  public void foo3() {            // Compliant
System.out.println();             // Non-Compliant
System.out.println();             // Compliant - already reported
System.out.println();             // Compliant - already reported

if (0) {                          // Compliant - already reported
  System.out.println();           // Non-Compliant
  if (0) {                        // Compliant - already reported
        System.out.println();     // Compliant
    System.out.println();         // Non-Compliant
  }

      ; System.out.println();     // Compliant
}
}

  class Foo {

    int a;                        // Compliant

  int b;                          // Non-Compliant

  }
}

enum Bar {
  A,
 B,
   C;

  public void foo1() {            // Compliant
  }

 public void foo2() {             // Non-Compliant
 }
}

interface Qix {

 void foo1();                     // Non-Compliant

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

 static class Qiz {                      // Non-Compliant
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
       while (keys.hasMoreElements()) {
        s = keys.nextElement();
        rId = (String) s;
        cName = (String) exceptionClassNames.get(rId);
        exceptionRepositoryIds.put (cName, rId);
      }
    } catch (NoSuchElementException e) { }
  }
}
