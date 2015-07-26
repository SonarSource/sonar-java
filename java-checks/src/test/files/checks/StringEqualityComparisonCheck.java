class Foo {
  void foo() {
    "foo" == variable;       // Noncompliant {{Replace "==" and "!=" by "equals()" and "!equals()" respectively to compare these strings.}}
    variable == "foo";       // Noncompliant {{Replace "==" and "!=" by "equals()" and "!equals()" respectively to compare these strings.}}
    "foo" != variable;       // Noncompliant {{Replace "==" and "!=" by "equals()" and "!equals()" respectively to compare these strings.}}
    variable != "foo";       // Noncompliant {{Replace "==" and "!=" by "equals()" and "!equals()" respectively to compare these strings.}}
    0 == variable == "foo";  // Noncompliant {{Replace "==" and "!=" by "equals()" and "!equals()" respectively to compare these strings.}}
    "foo".equals(variable);  // Compliant
    variable.equals("foo");  // Compliant
    0 == 0;                  // Compliant
    0 == variable;           // Compliant
    foo("bar") == 0;         // Compliant
  }
}
