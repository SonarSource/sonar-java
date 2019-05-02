class A {
  void myMethod(int x, int y, int z) {
    int j = 0, k = 0;
    for (int i = x; true; ) {}
    for (int i = x; false; ) {} // Noncompliant [[sc=21;ec=26]] {{This loop will never execute.}}
    for (int i = x; true; ) {}
    for (int i = x; !true; ) {} // Noncompliant
    for (int i = x; !(y=1); ) {}
    for (int i = 1; i < 5; ) {}
    for (int i = 9; i < 5; ) {} // Noncompliant
    for (int i = 9; i > 5; ) {}
    for (int i = 1; i > 5; ) {} // Noncompliant
    for (int i = 1; i <=5; ) {}
    for (int i = 9; i <=5; ) {} // Noncompliant
    for (int i = 9; i >=5; ) {}
    for (int i = 1; i >=5; ) {} // Noncompliant
    for (int i = x; i < 5; ) {}
    for (int i = 1; i < x; ) {}
    for (int i = 1; i <-x; ) {}
    for (         ; j < 5; ) {}
    for (    j = 9; j < 5; ) {} // Noncompliant
    for (   x += 1; j < 5; ) {}
    for (int i    ; i < 5; ) {}
    for (int i = 1;      ; ) {}
    for (int i = 0; i < 0x10; ) {}
    for (int i = 0; i < 0b10; ) {}
    for (int i = 1; i <= 0Xffff; i++) {}
  }
}
