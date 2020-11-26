import com.google.common.io.Closer;

class A extends Exception {
  private void f() {
    Closer closer = Closer.create();
    try {
    } catch (RuntimeException e) {    // Noncompliant {{Catch the proper exception instead of RuntimeException.}}
    } catch (Throwable e) {           // Noncompliant [[sc=14;ec=23]] {{Catch the proper exception instead of Throwable.}}
    } catch (Error e) {               // Noncompliant {{Catch the proper exception instead of Error.}}
    } catch (StackOverflowError e) {  // Compliant
    } catch (Foo |
      Error |                         // Noncompliant {{Catch the proper exception instead of Error.}}
      RuntimeException |              // Noncompliant {{Catch the proper exception instead of RuntimeException.}}
      Exception |                     // Noncompliant {{Catch the proper exception instead of Exception.}}
      Throwable                       // Noncompliant {{Catch the proper exception instead of Throwable.}}
      e) {
      try {
      } catch (Error e) {             // Noncompliant {{Catch the proper exception instead of Error.}}
      }
    } catch (java.lang.Throwable e) { // Noncompliant {{Catch the proper exception instead of Throwable.}}
    } catch (java.lang.Error e) {     // Noncompliant {{Catch the proper exception instead of Error.}}
    } catch (java.lang.Exception e) { // Noncompliant {{Catch the proper exception instead of Exception.}}
    } catch (java.lang.RuntimeException e) {     // Noncompliant {{Catch the proper exception instead of RuntimeException.}}
    } catch (Exception e) {           // Noncompliant {{Catch the proper exception instead of Exception.}}
    } catch (IOException ex) {        //Compliant
    } catch (foo.Throwable e) {       // Compliant
    } catch (java.foo.Throwable e) {  // Compliant
    } catch (foo.lang.Throwable e) {  // Compliant
    } catch (java.foo.RuntimeException e) {  // Compliant
    } catch (foo.lang.RuntimeException e) {  // Compliant
    } catch (java.foo.Exception e) {  // Compliant
    } catch (foo.lang.Exception e) {  // Compliant
    } catch (java.lang.foo e) {       // Compliant
    } catch (foo.java.lang.Throwable e) { // Compliant
    } catch (Throwable e) {           // Noncompliant {{Catch the proper exception instead of Throwable.}}
      throw e;
    } catch (Throwable e) {           // Noncompliant {{Catch the proper exception instead of Throwable.}}
      throw new Exception(e).getCause();
    } catch (Throwable e) {           // Compliant
      throw closer.rethrow(e);
    } catch (java.lang.Throwable e) { // Compliant
      throw closer.rethrow(e);
    } catch (Throwable e) {           // Noncompliant {{Catch the proper exception instead of Throwable.}}
      throw closer.rethrow(new Exception(e));
    } catch (Throwable e) {           // Noncompliant {{Catch the proper exception instead of Throwable.}}
      Throwable myThrowable = new Throwable(e);
      throw closer.rethrow(myThrowable);
    } catch (Throwable e) {           // Compliant
      throw closer.rethrow(e, A.class);
    } catch (Throwable e) {           // Compliant
      throw closer.rethrow(e, A.class, A.class);
    } catch (Throwable e) {           // Compliant
      throw closer.<A>rethrow(e, A.class);
    } catch (Throwable e) {           // Compliant
      throw closer.<A,A>rethrow(e, A.class, A.class);
    } finally {
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

  class ThrowingThrowable {
    public ThrowingThrowable() throws RuntimeException, java.lang.Throwable{
    }
  }

  private static void throwingThrowable() throws java.lang.Throwable {
  }
}
