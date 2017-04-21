package org.sonar.java.resolve.targets.nullableparameters;

class PackageAnnotations {
  void foo(Object input) {
    input.toString(); // Noncompliant {{A "NullPointerException" could be thrown; "input" is nullable here}}
  }

  private Integer bar1(int i) { return i; }
  private void qix1(int i) {
    bar1(i).intValue(); // Compliant - primitive types cannot be null, despite the fact that the package is annotated with "ParametersAreNullableByDefault"
  }

  private Integer bar2(Integer i) { return i; }
  private void qix2(Integer i) {
    bar2(i).intValue(); // Noncompliant {{A "NullPointerException" could be thrown; "bar2" is nullable here}}
  }
}
