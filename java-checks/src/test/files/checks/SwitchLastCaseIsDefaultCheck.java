class Foo {
  void foo() {
    switch (0) { // Noncompliant [[sc=5;ec=11]] {{Add a default case to this switch.}}
    }

    switch (0) { // Noncompliant
      case 0:
    }

    switch (0) { // Compliant
      default:
    }

    switch (0) {
      default:   // Noncompliant [[sc=7;ec=15]] {{Move this default to the end of the switch.}}
      case 0:
    }
  }
}
