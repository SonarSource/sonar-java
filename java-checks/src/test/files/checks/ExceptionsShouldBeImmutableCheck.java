class FooException extends RuntimeException {
  int foo; // Noncompliant {{Make this "foo" field final.}}
//    ^^^
  public final int bar;

  int a = 42, // Noncompliant
    b; // Noncompliant
}

class Foo extends RuntimeException {
  int foo; // Noncompliant {{Make this "foo" field final.}}
//    ^^^
}

class BarException {
  int foo; // Compliant
}

class FooError extends Error {
  private final Exception e = new Exception() {
    int foo; // Compliant - anonymous class
  };
  int foo; // Noncompliant
}

class FooBisError {
  int foo; // Compliant
}

class Bar extends Foo {
  int bar; // Noncompliant

  void method() {}
}
