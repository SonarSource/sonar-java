package checks;

abstract class DeadStoreCheck {

  void lambdas_not_resolved(UnknnownFunction lambda) {
    int a = 42; // Compliant
    lambdas_not_resolved(y -> a + y);
    lambdas_not_resolved(y -> {
      int x = 1; // Compliant
      return y + x;
    });
  }

  void fpLambdaWithAssignment(boolean cond) {
    String myStr;
    if (cond) {
      myStr = "A"; // Compliant, even if used in a unresolved lambda
    } else {
      myStr = "B"; //  Compliant, even if used in a unresolved lambda
    }
    new SomeUnknownClass(i -> unknown(i, myStr));
  }

  public void testCodeWithForLoop1() {
    RuntimeException e = null;
    for (;;) {
      for (int i = 0; i < 2; ) {
        try {
          e = new RuntimeException(); // Noncompliant
          break;
        } finally {
          doSomething();
        }
      }
    }
    // unreachable
    throw e;
  }

  void test_enquing_unknown_exceptions() throws FooException {
    int retriesLeft = 10;
    while (true) {
      try {
        bar();
      } catch (FooException e) {
        if (retriesLeft == 0) {
          throw e;
        }
      }
    }
  }

  Object anonymous_class() {
    int a,b = 7; // Noncompliant
    a = 42;
    if(a == 42) {
      b = 12; // Noncompliant
    }
    return new Object() {
      @Override
      public int hashCode() {
        b = 14; // Noncompliant
        return a;
      }
    };
  }

  abstract void bar() throws UnknownException;

  abstract void doSomething();

  public class FooException extends Exception { }
}
