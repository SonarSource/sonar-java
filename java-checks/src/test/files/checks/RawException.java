public class Example {

  public void throws_Throwable() throws Throwable { // Non-Compliant
    throw new Throwable(); // NOK
  }

  public void throws_Error() {
    throw new Error(); // NOK
  }

  public void throws_Exception() throws Exception { // Non-Compliant
    throw new Exception(); // NOK
  }

  public void throws_RuntimeException() {
    throw new RuntimeException(); // NOK
  }

  public void throws_custom() {
    throw new MyException(); // OK
  }

  class MyException extends RuntimeException { // Compliant
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
     Error,                   // Non-Compliant
     Exception {              // Non-Compliant
     throw new
         Throwable();         // Non-Compliant

     throw new int[0];        // Compliant
     }

  @Override
  public void throws_Exception() throws Exception { //Compliant because of overrides

  }

  @Deprecated
  public void throws_Exception() throws Exception {
  }
}
