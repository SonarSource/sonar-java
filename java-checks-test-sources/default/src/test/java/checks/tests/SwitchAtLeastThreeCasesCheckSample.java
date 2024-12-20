package checks.tests;

public class SwitchAtLeastThreeCasesCheckSample {
  record MyRecord(int x, int y) {}

  static void recordSwitch1(Object object) {
    switch (object) { // Compliant
      case String x when x.length() > 42 -> { }
      default -> { }
    }
  }

  static void recordSwitch2(Object object) {
    switch (object) { // Compliant
      case MyRecord(int x, int y) -> { }
      default -> { }
    }
  }

  public interface SealedClass {
    sealed interface Shape permits Box, Circle {}
    record Box() implements Shape { }
    record Circle() implements Shape {}

    default void foo(Shape shape) {
      switch (shape) { // Compliant because of type pattern matching
        case Box ignored -> { }
        case Circle ignored -> System.out.println();
      }
    }

    default void goo(Shape shape) {
      switch (shape) { // Compliant because of type pattern matching
        case Box ignored -> { }
        default -> System.out.println();
      }
    }
  }

  private static void doSomething() {}
  private static void doSomethingElse() {}

  public void f(int variable) {
    switch (variable) { // Noncompliant {{Replace this "switch" statement by "if" statements to increase readability.}}
//  ^^^^^^
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

    switch (variable) {
      case 0, 1:
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
