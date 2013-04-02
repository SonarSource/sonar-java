public class Example {

  public void throws_Throwable() throws Throwable {
    throw new Throwable(); // NOK
  }

  public void throws_Error() {
    throw new Error(); // NOK
  }

  public void throws_Exception() throws Exception {
    throw new Exception(); // NOK
  }

  public void throws_RuntimeException() {
    throw new RuntimeException(); // NOK
  }

  public void throws_custom() {
    throw new MyException(); // OK
  }

  class MyException extends RuntimeException {
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

}
