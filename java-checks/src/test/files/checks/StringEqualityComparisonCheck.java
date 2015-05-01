class Foo {
  void foo() {
    "foo" == variable;       // Noncompliant
    variable == "foo";       // Noncompliant
    "foo" != variable;       // Noncompliant
    variable != "foo";       // Noncompliant
    0 == variable == "foo";  // Noncompliant
    "foo".equals(variable);  // Compliant
    variable.equals("foo");  // Compliant
    0 == 0;                  // Compliant
    0 == variable;           // Compliant
    foo("bar") == 0;         // Compliant
  }
}
