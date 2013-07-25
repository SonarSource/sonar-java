class A {
  int a = false ? (true ? (false ? 1 : 0) : 0) : 1;                   // Compliant
  int b = false ? (true ? (false ? (true ? 1 : 0) : 0) : 0) : 1;      // Non-Compliant

  int c = true || false || true || false || false;                    // Non-Compliant
  int d = true && false && true && false && true && true;             // Non-Compliant

  int e = true | false | true | false;                                // Compliant

  void f() {
    if ((true ? 0 : 1) || false || true && false && true || false) {  // Non-Compliant
    }
  }
}
