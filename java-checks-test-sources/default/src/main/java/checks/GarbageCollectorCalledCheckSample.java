package checks;

class GarbageCollectorCalledCheckSample {

  Object o;
  Foo foo;

  class Foo {
    void gc() {}
    void runFinalization() {}

    Runtime getRuntime() {
      return Runtime.getRuntime();
    }
  }

  private Runtime foo() {
    return Runtime.getRuntime();
  }

  private void f() {
    System.gc(); // Noncompliant [[sc=12;ec=14]] {{Don't try to be smarter than the JVM, remove this call to run the garbage collector.}}
    System.runFinalization(); // Noncompliant [[sc=12;ec=27]] {{Don't try to be smarter than the JVM, remove this call to run the garbage collector.}}
    foo.gc(); // Compliant
    System.exit(0); // Compliant
    Runtime.getRuntime().gc(); // Noncompliant
    Runtime.getRuntime().runFinalization(); // Noncompliant
    foo.getRuntime().gc(); // Noncompliant
    foo.getRuntime().runFinalization(); // Noncompliant
    foo().gc(); // Noncompliant
    foo().runFinalization(); // Noncompliant
    (foo()).gc(); // Noncompliant
    (foo()).runFinalization(); // Noncompliant
  }
}
