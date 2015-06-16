class Foo {
  void foo() {
    switch (0) { // Noncompliant {{Add a default case to this switch.}}
    }

    switch (0) { // Noncompliant
      case 0:
    }

    switch (0) { // Compliant
      default:
    }

    switch (0) {
      default:   // Noncompliant {{Move this default to the end of the switch.}}
      case 0:
    }
  }
}
