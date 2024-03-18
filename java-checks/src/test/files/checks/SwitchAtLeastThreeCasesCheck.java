class A {

  static void recordSwitch1(MyRecord object) {
    switch (object) { // Compliant
      case String x when x.length() > 42 -> { }
      case Integer i -> { }
    }
  }

  static void recordSwitch1(Object object) {
    switch (object) { // Compliant
      case MyRecord(int x, int y) -> { }
      case String s -> { }
    }
  }

  public interface SealedClass {
    sealed interface Shape permits Box, Circle {}
    record Box() implements Shape { }
    record Circle() implements Shape {}

    void foo(Shape shape) {
      switch (shape) { // Compliant because of type pattern matching
        case Box ignored -> { }
        case Circle ignored -> System.out.println();
      }
    }

    void goo(Shape shape) {
      switch (shape) { // Compliant because of type pattern matching
        default -> System.out.println();
        case Box ignored -> { }
      }
    }
  }

  public void f() {
    switch (variable) { // Noncompliant [[sc=5;ec=11]] {{Replace this "switch" statement by "if" statements to increase readability.}}
      case 0:
        doSomething();
        break;
      default:
        doSomethingElse();
        break;
    }

    switch (variable) {
      case 0:
      case 1:
        doSomething();
        break;
      default:
        doSomethingElse();
        break;
    }

    switch (variable) { // Noncompliant
    }

    if (variable == 0) {
      doSomething();
    } else {
      doSomethingElse();
    }
  }
}
