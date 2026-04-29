package checks;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedActionException;

public class RawExceptionCheckSample {

  public void throws_Throwable() throws Throwable { // Noncompliant
//                                      ^^^^^^^^^
    throw new Throwable(); // Noncompliant
  }

  public void method_throwing_throwable() throws Throwable { // Compliant
    throwingExceptionMethod1();
  }

  private void throwingExceptionMethod1() throws Throwable { // Noncompliant
//                                               ^^^^^^^^^
  }

  private void throwingExceptionMethod2() throws java.lang.Throwable { // Noncompliant
//                                               ^^^^^^^^^^^^^^^^^^^
  }

  public void throws_Error() {
    throw new Error(); // Noncompliant
//            ^^^^^
  }

  public void throws_Exception() throws Exception { // Noncompliant {{Replace generic exceptions with specific library exceptions or a custom exception.}}
    throw new Exception(); // Noncompliant
  }

  public void throws_RuntimeException() {
    throw new RuntimeException(); // Noncompliant
  }

  public String wraps_specific_checked_exception() {
    try {
      return readFromBlockingSource();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e); // Compliant
    }
  }

  public String wraps_specific_checked_exception_with_message() {
    try {
      return readFromBlockingSource();
    } catch (IOException e) {
      throw new RuntimeException("Failed to read", e); // Compliant
    } catch (InterruptedException e) {
      throw new RuntimeException(e); // Compliant
    }
  }

  public String wraps_direct_derived_checked_exception_cause() {
    try {
      return readPrivilegedValue();
    } catch (PrivilegedActionException e) {
      throw new RuntimeException("Failed to read", e.getCause()); // Compliant
    }
  }

  public String wraps_local_derived_checked_exception_cause() {
    try {
      return invokeReflectively();
    } catch (InvocationTargetException e) {
      Throwable target = e.getTargetException();
      throw new Error("Failed to invoke", target); // Compliant
    }
  }

  public String wraps_generic_exception() {
    try {
      return readFromBlockingSource();
    } catch (Exception e) {
      throw new RuntimeException(e); // Noncompliant
    }
  }

  public String wraps_throwable() {
    try {
      return readFromBlockingSource();
    } catch (Throwable e) {
      throw new RuntimeException(e); // Noncompliant
    }
  }

  public String wraps_unchecked_exception() {
    try {
      return readFromBlockingSource();
    } catch (IllegalStateException e) {
      throw new RuntimeException(e); // Noncompliant
    } catch (IOException | InterruptedException e) {
      return "";
    }
  }

  public String wraps_mixed_checked_and_unchecked_exceptions() {
    try {
      return readFromBlockingSource();
    } catch (IOException | IllegalStateException e) {
      throw new RuntimeException(e); // Noncompliant
    } catch (InterruptedException e) {
      throw new RuntimeException(e); // Compliant
    }
  }

  public String wraps_checked_exception_without_cause() {
    try {
      return readFromBlockingSource();
    } catch (IOException e) {
      throw new RuntimeException("Failed to read"); // Noncompliant
    } catch (InterruptedException e) {
      throw new RuntimeException(e); // Compliant
    }
  }

  public String wraps_unrelated_throwable() {
    try {
      return readFromBlockingSource();
    } catch (IOException e) {
      Throwable unrelated = new InterruptedException();
      throw new RuntimeException(unrelated); // Noncompliant
    } catch (InterruptedException e) {
      throw new RuntimeException(e); // Compliant
    }
  }

  private String readFromBlockingSource() throws IOException, InterruptedException {
    return "";
  }

  private String readPrivilegedValue() throws PrivilegedActionException {
    return "";
  }

  private String invokeReflectively() throws InvocationTargetException {
    return "";
  }

  public void throws_custom() throws MyOtherException { // Compliant
    throw new MyException(); // OK
  }

  class MyException extends RuntimeException { // Compliant
  }

  class MyOtherException extends Exception { // Compliant
  }

  public void throws_value() {
    throw create(); // OK
  }

  public RuntimeException create() {
    return new RuntimeException(); // OK
  }

  public void exception() {
    try {
      throws_Exception();
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e; // OK
      }
    }
  }

  public RawExceptionCheckSample() throws
     Throwable, // Noncompliant
     Error, // Noncompliant
     Exception { // Noncompliant {{Replace generic exceptions with specific library exceptions or a custom exception.}}
     throw new
         Throwable(); // Noncompliant

     }

  @Deprecated
  public void throws_Exception2() throws Exception { // Noncompliant
  }

  private class Nested {
    Nested() throws Exception {
      throws_Exception();
    }
  }

  void forcedException() throws Exception { // Compliant - Nested() can throw Exception
    new Nested();
  }
}
class SubClass extends RawExceptionCheckSample {

  public SubClass() throws Error,
    Throwable,
    Exception {
    super();
  }

  @Override
  public void throws_Exception() throws Exception { // Compliant because overrides.
    super.throws_Exception();
  }

  @Override
  @Deprecated
  public void throws_Exception2() throws Exception { // Compliant because overrides.
  }

  public static void main(String[] args) throws Exception { //should not raise issue SONARJAVA-671
  }

  void main() throws Exception { // Compliant, because it is an instance main.
  }
}
