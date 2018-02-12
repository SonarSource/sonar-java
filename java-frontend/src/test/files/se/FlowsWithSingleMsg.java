package org.sonar.java.resolve.targets;

class A {

  int div() {
    return 1 / 0; // Noncompliant
  }

  void ternary(Object r1, Object r2) {
    if (!false) { // Noncompliant

    }
  }

  void ternary(Object r1, Object r2) {
    if (!true) { // Noncompliant

    }
  }

  void nully() {
    null.toString(); // Noncompliant
  }

}
