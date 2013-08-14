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

enum Foo {
  A,
 B,
   C;

  public void foo1() {            // Compliant
  }

 public void foo2() {             // Non-Compliant
 }
}

interface Foo {

 void foo1();                     // Non-Compliant

  void foo2();                    // Compliant

}

class Foo {

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

 class Foo {                      // Non-Compliant
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
  };
}
