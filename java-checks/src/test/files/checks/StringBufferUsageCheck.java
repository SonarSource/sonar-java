class Foo {
  StringBuffer sb1;   // Non-Compliant
  StringBuilder sb2;  // Compliant

  void foo() {
    StringBuffer sb = new StringBuffer(); // Non-Compliant - expect a single violation
  }
}
