package symbolicexecution.checks;

public class ConditionAlwaysTrueOrFalseCheckParentLoop {
  boolean active;
  private Foo foo = null;

  public void run() {
    active = true;
    while (active) { // Noncompliant FP
      try {
        foo.method();
      } catch (CustomException se) {
      }
    }
  }

  private class Foo {
    public void method() throws CustomException {
    }
  }
  class CustomException extends Exception {
  }
}
