class A {
  A() {
    System.exit(0); // Noncompliant {{Remove this call to "exit" or ensure it is really required.}}
  }

  void f() {
    System.exit(0);          // Noncompliant {{Remove this call to "exit" or ensure it is really required.}}
    int a = System.exit(0);  // Noncompliant [[sc=13;ec=24]] {{Remove this call to "exit" or ensure it is really required.}}
    System.gc();             // Compliant
    exit();                  // Compliant
    Runtime.getRuntime().exit(); // Noncompliant [[sc=5;ec=30]] {{Remove this call to "exit" or ensure it is really required.}}
    Object o = Runtime.getRuntime().foo;    // Compliant
    Runtime.getRuntime().foo();  // Compliant
    Runtime.getRuntime().halt(12); // Noncompliant {{Remove this call to "halt" or ensure it is really required.}}
  }

  public static void main(String[] args) {
    Runtime.getRuntime().halt(12); // Compliant
    Runtime.getRuntime().exit();   // Compliant
    System.exit(0);                // Compliant
  }
}
