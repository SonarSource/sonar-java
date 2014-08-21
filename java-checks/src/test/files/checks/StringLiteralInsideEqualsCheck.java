class Foo {
  void foo() {
    "foo".equals("bar");        // Non-Compliant
    foo.equals("qux");          // Non-Compliant
    "foo".equals(bar);          // Compliant
    "foo".foo("bar");           // Compliant
    "foo".equals();             // Compliant
    foo.equals("bar".length()); // Compliant
    "foo".equals[0];            // Compliant
    int a = foo.equals;         // Compliant
    "foo".equalsIgnoreCase(""); // Non-Compliant
    StringUtils.equals("", ""); // Compliant

    foo()
    .bar().
    equals
    ("");                       // Noncompliant
  }
}
