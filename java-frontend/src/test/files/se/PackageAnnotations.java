package org.sonar.java.resolve.targets;

class PackageAnnotations {
  void foo(Object input) {
    if(input == null) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}

    }
  }
}