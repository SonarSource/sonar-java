class A {
  void f() {
    System.exit(0);          // Noncompliant {{Remove this call to "exit" or ensure it is really required.}}
    int a = System.exit(0);  // Noncompliant {{Remove this call to "exit" or ensure it is really required.}}
    System.gc();             // Compliant
    System.exit[0];          // Compliant
    exit();                  // Compliant
    Runtime.getRuntime().exit(); // Noncompliant {{Remove this call to "exit" or ensure it is really required.}}
    Runtime.getRuntime().foo;    // Compliant
    Runtime.getRuntime().foo();  // Compliant
    Runtime.getRuntime()++;      // Compliant
    Runtime.getRuntime().halt(12); // Noncompliant {{Remove this call to "halt" or ensure it is really required.}}
  }
  
  public static void main(String[] args) {
    Runtime.getRuntime().halt(12); // Compliant
    Runtime.getRuntime().exit();   // Compliant
    System.exit(0);                // Compliant
  }
}
