class A {
  private void f() {
    try {
    } catch (RuntimeException e) {    // Compliant
    } catch (Throwable e) {           // Non-Compliant
    } catch (Error e) {               // Non-Compliant
    } catch (StackOverflowError e) {  // Compliant
    } catch (Foo |
        Error |                       // Non-Compliant
        RuntimeException e) {

      try {
      } catch (Error e) {             // Noncompliant
      }
    } catch (java.lang.Throwable e) { // Compliant - limitation
    } finally {
    }
  }
}
