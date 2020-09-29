package org.foo;

class A {
  void foo() {
    unknownTypeMethod();// Compliant type is unknown
    unresolvedMethod();// Compliant method is not resolved so type is unknown
    fluentMethod(""); // Compliant
    bar(); // Compliant

    Integer.valueOf("1").byteValue(); // Noncompliant {{The return value of "byteValue" must be used.}}
  }

  UnknownType bar();
}
