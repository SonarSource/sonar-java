class Foo {
  void foo() {
    return 3;             // Compliant
    return (x);           // Non-Compliant
    return (x + 1);       // Non-Compliant
    int x = (y / 2 + 1);  // Non-Compliant
    int y = (4+X) * y;    // Compliant

    if (0) {              // Compliant
    }
  }
}
