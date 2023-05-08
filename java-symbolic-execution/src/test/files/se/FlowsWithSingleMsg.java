package org.sonar.java.resolve.targets;

class A {

  int div() {
    return 1 / 0; // Noncompliant
  }

  void ternary1(Object r1, Object r2) {
    if (!false) { // Noncompliant

    }
  }

  void ternary2(Object r1, Object r2) {
    if (!true) { // Noncompliant

    }
  }

  void nully() {
    ((Object) null).toString(); // Noncompliant
  }

}
