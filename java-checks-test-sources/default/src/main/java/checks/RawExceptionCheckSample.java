package checks;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedActionException;

public class RawExceptionCheckSample {


  public String wrapsDirectDerivedCheckedExceptionCause1() {
    try {
      return readPrivilegedValue();
    } catch (PrivilegedActionException e) {
      throw new RuntimeException("Failed to read", e.getCause()); // Compliant
    }
  }

  public void throwsThrowable() throws Throwable { // Noncompliant
//                                     ^^^^^^^^^
    throw new Throwable(); // Noncompliant
  }

  public void methodThrowingThrowable() throws Throwable { // Compliant
    throwingExceptionMethod1();
  }

  private void throwingExceptionMethod1() throws Throwable { // Noncompliant
//                                               ^^^^^^^^^
  }

  private void throwingExceptionMethod2() throws java.lang.Throwable { // Noncompliant
//                                               ^^^^^^^^^^^^^^^^^^^
  }

  public void throwsError() {
    throw new Error(); // Noncompliant
//            ^^^^^
  }

  public void throwsException() throws Exception { // Noncompliant {{Replace generic exceptions with specific library exceptions or a custom exception.}}
    throw new Exception(); // Noncompliant
  }

  public void throwsRuntimeException() {
    throw new RuntimeException(); // Noncompliant
  }

  public String wrapsSpecificCheckedException() {
    try {
      return readFromBlockingSource();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e); // Compliant
    }
  }

  public String wrapsSpecificCheckedExceptionWithMessage() {
    try {
      return readFromBlockingSource();
    } catch (IOException e) {
      throw new RuntimeException("Failed to read", e); // Compliant
    } catch (InterruptedException e) {
      throw new RuntimeException(e); // Compliant
    }
  }

  public String wrapsDirectDerivedCheckedExceptionCause() {
    try {
      return readPrivilegedValue();
    } catch (PrivilegedActionException e) {
      throw new RuntimeException("Failed to read", e.getCause()); // Compliant
    }
  }

  public String wrapsLocalDerivedCheckedExceptionCause() {
    try {
      return invokeReflectively();
    } catch (InvocationTargetException e) {
      Throwable target = e.getTargetException();
      throw new Error("Failed to invoke", target); // Compliant
    }
  }

  public String wrapsUncheckedException() {
    try {
      return readFromBlockingSource();
    } catch (IllegalStateException e) {
      throw new RuntimeException(e);
    } catch (IOException | InterruptedException e) {
      return "";
    }
  }

  public String wrapsCheckedExceptionWithoutCause() {
    try {
      return readFromBlockingSource();
    } catch (IOException e) {
      throw new RuntimeException("Failed to read"); // Noncompliant
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

  public void throwsCustom() throws MyOtherException { // Compliant
    throw new MyException(); // OK
  }

  class MyException extends RuntimeException { // Compliant
  }

  class MyOtherException extends Exception { // Compliant
  }

  public void throwsValue() {
    throw create(); // OK
  }

  public RuntimeException create() {
    return new RuntimeException(); // OK
  }

  public void exception() {
    try {
      throwsException();
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
  public void throwsException2() throws Exception { // Noncompliant
  }

  private class Nested {
    Nested() throws Exception {
      throwsException();
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
  public void throwsException() throws Exception { // Compliant because overrides.
    super.throwsException();
  }

  @Override
  @Deprecated
  public void throwsException2() throws Exception { // Compliant because overrides.
  }

  public static void main(String[] args) throws Exception { //should not raise issue SONARJAVA-671
  }

  void main() throws Exception { // Compliant, because it is an instance main.
  }
}
