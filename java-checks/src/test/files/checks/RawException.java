public class Example {

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

  public Exception create() {
    return new RuntimeException(); // OK
  }

  public Exception() {
    try {
      throws_exception();
    } catch (Exception e) {
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e; // OK
      }
    }
  }

  public Example() throws
     Error,                   // Noncompliant
     Exception {              // Noncompliant {{Define and throw a dedicated exception instead of using a generic one.}}
     throw new
         Throwable();         // Noncompliant

     throw new int[0];        // Compliant
     }

  @Override
  public void throws_Exception() throws Exception { //Compliant because of overrides

  }

  @Deprecated
  public void throws_Exception() throws Exception { // Noncompliant
  }
}
class SubClass extends Example {

  public void throws_Exception() throws Exception { //Compliant because overrides.
    super.throws_Exception();
  }
  public static void main(String[] args) throws Exception { //should not raise issue SONARJAVA-671
  }
}
