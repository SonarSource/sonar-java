package org.sonar.java.resolve.targets.nullableparameters;

class PackageAnnotations {
  void foo(Object input) {
    input.toString(); // Noncompliant {{NullPointerException might be thrown as 'input' is nullable here}}
  }
}
