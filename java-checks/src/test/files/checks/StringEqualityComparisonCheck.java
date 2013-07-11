class Foo {
  void foo() {
    "foo" == variable;       // Non-Compliant
    variable == "foo";       // Non-Compliant
    "foo" != variable;       // Non-Compliant
    variable != "foo";       // Non-Compliant
    0 == variable == "foo";  // Non-Compliant
    "foo".equals(variable);  // Compliant
    variable.equals("foo");  // Compliant
    0 == 0;                  // Compliant
    0 == variable;           // Compliant
    foo("bar") == 0;         // Compliant
  }
}
