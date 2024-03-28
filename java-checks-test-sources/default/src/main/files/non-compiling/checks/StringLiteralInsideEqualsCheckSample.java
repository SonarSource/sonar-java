package checks;

class StringLiteralInsideEqualsCheckSample {
  void foo() {
    unknown.equals("qux"); // Noncompliant
    "foo".equals(unknown);
    "foo".foo("bar");
    "foo".equals();
    unknown.equals("bar".length());
    int a = foo.equals;
    StringUtils.equals("", "");

    foo()
      .bar()
      .equals(""); // Noncompliant
  }
}
