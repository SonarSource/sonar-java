class A {
  private void f() {
    System.gc(); // Noncompliant {{Don't try to be smarter than the JVM, remove this call to run the garbage collector.}}
    foo.gc(); // Compliant
    System.exit(0); // Compliant
    System.gc; // Compliant
    System.gc[0]; // Compliant
    Runtime.getRuntime().gc(); // Noncompliant
  }
}
