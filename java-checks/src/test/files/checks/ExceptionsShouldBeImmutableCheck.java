class FooException extends RuntimeException {
  int foo; // Noncompliant {{Make this "foo" field final.}}
  public final int bar;

  int a = 42, // Noncompliant
    b; // Noncompliant
}

class Foo extends RuntimeException {
  int foo;
}

class BarException {
  int foo; // Noncompliant
}

class FooError extends Error {
  int foo; // Noncompliant
}

class FooBisError {
  int foo; // Noncompliant
}
