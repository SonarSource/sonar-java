package checks;

public class RawExceptionCheck {

  public void throws_Throwable() throws Throwable { // Noncompliant [[sc=41;ec=50]]
    throw new Throwable(); // Noncompliant
  }

  public void method_throwing_throwable() throws Throwable { // Compliant
    throwingExceptionMethod1();
  }

  private void throwingExceptionMethod1() throws Throwable { // Noncompliant [[sc=50;ec=59]]
  }

  private void throwingExceptionMethod2() throws java.lang.Throwable { // Noncompliant [[sc=50;ec=69]]
  }

  public void throws_Error() {
    throw new Error(); // Noncompliant [[sc=15;ec=20]]
  }

  public void throws_Exception() throws Exception { // Noncompliant {{Define and throw a dedicated exception instead of using a generic one.}}
    throw new Exception(); // Noncompliant
  }

  public void throws_RuntimeException() {
    throw new RuntimeException(); // Noncompliant
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

  public RawExceptionCheck() throws
     Throwable,               // Noncompliant
     Error,                   // Noncompliant
     Exception {              // Noncompliant {{Define and throw a dedicated exception instead of using a generic one.}}
     throw new
         Throwable();         // Noncompliant

     }

  @Deprecated
  public void throws_Exception2() throws Exception { // Noncompliant
  }
}
class SubClass extends RawExceptionCheck {

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
}
