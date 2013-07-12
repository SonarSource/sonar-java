class Foo {
  int a;                           // Compliant
  int b;;                          // Non-Compliant
  ;                                // Non-Compliant

  void foo() {
    for (int i = 0; i < 42; i++);  // Non-Compliant
    int i = 0;;                    // Non-Compliant
    ;                              // Non-Compliant

    int a = 0;                     // Compliant
    a = 42;                        // Compliant

    for (;;) {}                    // Compliant
  }
}