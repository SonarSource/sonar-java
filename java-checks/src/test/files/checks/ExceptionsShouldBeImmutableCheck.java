class FooException extends RuntimeException {
  int foo; // Non-Compliant
  public final int bar; // Compliant
}

class Foo extends RuntimeException {
  int foo; // Compliant - limitation
}

class BarException {
  int foo; // Non-Compliant - limitation
}
