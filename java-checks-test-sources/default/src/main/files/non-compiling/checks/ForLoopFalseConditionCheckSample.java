class A {
  void myMethod(int x, int y, int z) {
    int j = 0, k = 0;

    for (int i = x; !(y=1); ) {}

    for (int i    ; i < 5; ) {}

    for (int i = x; false; ) {} // Noncompliant [[sc=21;ec=26]] {{This loop will never execute.}}

    for (int i = x; !true; ) {} // Noncompliant

  }
}
