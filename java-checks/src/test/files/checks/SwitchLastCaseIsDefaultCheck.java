class Foo {
  void foo() {
    switch (0) { // Non-Compliant
    }

    switch (0) { // Non-Compliant
      case 0:
    }

    switch (0) { // Compliant
      default:
    }

    switch (0) {
      default:   // Non-Compliant
      case 0:
    }
  }
}
