
class A {
  void nonCompliant() {
    String name = "ismael";

    if (name.indexOf("ae") > 2) { // Noncompliant
      // ...
    }
    if (name.indexOf("ae") > 1) { // Noncompliant
      // ...
    }
    if (2 < (name.indexOf("ae"))) { // Noncompliant
      // ...
    }
  }

  int value() {}

  void compliant() {
    String name = "ismael";

    if (name.indexOf("ae", 2) > -1) {
      // ...
    }
    if (-1 < name.indexOf("ae", 2)) {
      // ...
    }
    if (name.indexOf("ae") > -1) {

    }
    if (name.indexOf("is") == 0) {

    }
    if (name.indexOf("ae") > value()) {

    }

    // coverage
    1 > 2;
  }
}
