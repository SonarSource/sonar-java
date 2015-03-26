class A {
  private void f() {
    try {
    } catch (RuntimeException e) {    // Compliant
    } catch (Throwable e) {           // Non-Compliant
    } catch (final Throwable e) {     // Non-Compliant
    } catch (Error e) {               // Non-Compliant
    } catch (StackOverflowError e) {  // Compliant
    } catch (Foo |
        Error |                       // Non-Compliant
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
    } finally {
    }
  }
}
