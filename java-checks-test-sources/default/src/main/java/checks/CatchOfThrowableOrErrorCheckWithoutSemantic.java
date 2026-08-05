package checks;

import com.google.common.io.Closer;
import java.util.MissingResourceException;

class CatchOfThrowableOrErrorCheckWithoutSemantic {

  class A extends Exception {
    private void f() throws Exception {
      Closer closer = Closer.create();
      try {
      } catch (RuntimeException e) {    // Compliant
      } catch (Throwable e) { // Noncompliant

      }
      try {
      } catch (Error e) { // Noncompliant
      }
      try {
      } catch (StackOverflowError f) {  // Compliant
      }
      try {
      } catch (ArithmeticException |
        Error | // Noncompliant
        MissingResourceException g) {
      }
      try {
      } catch (Throwable f) { // Noncompliant
      }

      try {
      } catch (java.lang.Throwable e) { // Noncompliant
      }
      try {
      } catch (java.lang.Error e) { // Noncompliant
      }
      try {
      } catch (Throwable e) { // Noncompliant
        throw e;
      }
      try {
      } catch (Throwable e) { // Noncompliant
        throw new Exception(e);
      }
      try {
      } catch (Throwable e) {           // Compliant
        throw closer.rethrow(e);
      }
      try {
      } catch (java.lang.Throwable e) { // Compliant
        throw closer.rethrow(e);
      }
      try {
      } catch (Throwable e) { // FN
        throw closer.rethrow(new Exception(e));
      }
      try {
      } catch (Throwable e) { // FN
        Throwable myThrowable = new Throwable(e);
        throw closer.rethrow(myThrowable);
      }
      try {
      } catch (Throwable e) {           // Compliant
        throw closer.rethrow(e, A.class);
      }
      try {
      } catch (Throwable e) {           // Compliant
        throw closer.rethrow(e, A.class, A.class);
      }
      try {
      } catch (Throwable e) {           // Compliant
        throw closer.<A>rethrow(e, A.class);
      }
      try {
      } catch (Throwable e) {           // Compliant
        throw closer.<A, A>rethrow(e, A.class, A.class);
      } finally {
      }
    }

    private void exception2() {
      try {
        throwingThrowable();
        throwingThrowable();
      } catch (java.lang.Throwable e) {

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
