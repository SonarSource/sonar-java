package checks;

import com.google.common.io.Closer;

class CatchOfThrowableOrErrorCheck {

  class A extends Exception {
    private void f() throws Exception {

      try {} catch (Throwable e) {           // Compliant
        throw closer.rethrow(e);
      }
      try {} catch (java.lang.Throwable e) { // Compliant
        throw closer.rethrow(e);
      }
      try {} catch (Throwable e) {           // Compliant
        throw closer.rethrow(new Exception(e));
      }
      try {} catch (Throwable e) {           // Compliant
        Throwable myThrowable = new Throwable(e);
        throw closer.rethrow(myThrowable);
      }
      try {} catch (Throwable e) {           // Compliant
        throw closer.rethrow(e, A.class);
      }
      try {} catch (Throwable e) {           // Compliant
        throw closer.rethrow(e, A.class, A.class);
      }
      try {} catch (Throwable e) {           // Compliant
        throw closer.<A>rethrow(e, A.class);
      }
      try {} catch (Throwable e) {           // Compliant
        throw closer.<A, A>rethrow(e, A.class, A.class);
      }
    }

    private void exception1() {
      try {
        unknown();
        f();
        new ThrowingThrowable();
      } catch (java.lang.Throwable e) {

      }
    }

    private void exception2() {
      try {
        throwingThrowable();
        throwingThrowable();
      } catch (java.lang.Throwable e) {

      }
    }

    private void doNotReportWhenUnknownMethodInTryBlock() {
      try {
        unknown();
      } catch (java.lang.Throwable e) { // Compliant with incomplete semantics, we cannot asess whether unknown will throw something.
      }
      try {
        UnknownType o = new UnknownType();
      } catch (java.lang.Throwable e) { // Compliant with incomplete semantics, we cannot asess whether unknown will throw something.
      }
      try {
        return unknown();
      } catch (java.lang.Throwable e) { // Compliant with incomplete semantics, we cannot asess whether unknown will throw something.
      }
    }

    class ThrowingThrowable {
      public ThrowingThrowable() throws RuntimeException, java.lang.Throwable {
      }
    }

    private static void throwingThrowable() throws java.lang.Throwable {
    }
  }
}
