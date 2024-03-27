class A {

  Object o;

  private void f() {
    System.gc(); // Noncompliant [[sc=12;ec=14]] {{Don't try to be smarter than the JVM, remove this call to run the garbage collector.}}
    foo.gc(); // Compliant
    System.exit(0); // Compliant
    o = System.gc; // Compliant
    o = System.gc[0]; // Compliant
    Runtime.getRuntime().gc(); // Noncompliant
    Runtime.getFoo().gc();
    Runtime.getRuntime(foo).gc();
    Foo.getRuntime().gc();
    foo().getRuntime().gc();
    foo().gc();
    (foo()).gc();
  }
}
