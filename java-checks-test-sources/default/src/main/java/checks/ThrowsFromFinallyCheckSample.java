package checks;

class ThrowsFromFinallyCheckSample {

  {
    if (foo()) {
      throw new IllegalAccessError();
    }
  }

  public ThrowsFromFinallyCheckSample() {
    throw new IllegalAccessError();
  }

  boolean foo() {
    return true;
  }

  public void f() {
    if (foo()) {
      throw new IllegalAccessError();
    }

    try {
      throw new IllegalAccessError();
    } catch (Exception e) {
      throw new IllegalAccessError();
    } finally {
      throw new IllegalAccessError(); // Noncompliant {{Refactor this code to not throw exceptions in finally blocks.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  public void g() {
    try {
      throw new IllegalAccessError();
    } catch (Exception e) {
      throw new IllegalAccessError();
    } finally {
      if (foo()) {
        throw new IllegalAccessError(); // Noncompliant
      }

      new ThrowsFromFinallyCheckSample() {
        public void f() {
          throw new IllegalAccessError();
        }
      };
      try { } catch (Exception e){ }
      throw new IllegalAccessError(); // Noncompliant
    }
  }

  public interface B {
    public default void f() {
      throw new IllegalAccessError();
    }
  }
}
