import com.google.common.io.Closer;

class A {
  private void f() {
    Closer closer = Closer.create();
    try {
    } catch (RuntimeException e) {    // Compliant
    } catch (Throwable e) {           // Noncompliant
    } catch (Error e) {               // Noncompliant
    } catch (StackOverflowError e) {  // Compliant
    } catch (Foo |
        Error |                       // Noncompliant
        RuntimeException e) {
      try {
      } catch (Error e) {             // Noncompliant
      }
    } catch (java.lang.Throwable e) { // Noncompliant
    } catch (java.lang.Error e) {     // Noncompliant
    } catch (foo.Throwable e) {       // Compliant
    } catch (java.foo.Throwable e) {  // Compliant
    } catch (foo.lang.Throwable e) {  // Compliant
    } catch (java.lang.foo e) {       // Compliant
    } catch (foo.java.lang.Throwable e) { // Compliant
    } catch (Throwable e) {           // Noncompliant
      throw e;
    } catch (Throwable e) {           // Noncompliant
      throw new Exception(e).getCause();
    } catch (Throwable e) {           // Compliant
      throw closer.rethrow(e);
    } catch (java.lang.Throwable e) { // Compliant
      throw closer.rethrow(e);
    } catch (Throwable e) {           // Noncompliant
      throw closer.rethrow(new Exception(e));
    } catch (Throwable e) {           // Noncompliant
      Throwable myThrowable = new Throwable(e);
      throw closer.rethrow(myThrowable);
    } catch (Throwable e) {           // Compliant
      throw closer.rethrow(e, A.class);
    } catch (Throwable e) {           // Compliant
      throw closer.rethrow(e, A.class, A.class);
    } finally {
    }
  }
}
