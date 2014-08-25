class Foo {
  void foo() {
    return 3;             // Compliant
    return (x);           // Non-Compliant
    return (x + 1);       // Non-Compliant
    int x = (y / 2 + 1);  // Non-Compliant
    int y = (4+X) * y;    // Compliant

    if (0) {              // Compliant
    }

    System.out.println(false ? (true ? 1 : 2) : 2); // Was previously noncompliant
    System.out.println(false ? 0 : (true ? 1 : 2)); // Was previously compliant
  }
}
