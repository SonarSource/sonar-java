class A {
  public A() { // Noncompliant [[sc=10;ec=11]] {{This method has 6 lines, which is greater than the 4 lines authorized. Split it into smaller methods.}}
    // 2
    // 3
    // 4
    // 5
  } // 6

  public void f() { // Noncompliant {{This method has 5 lines, which is greater than the 4 lines authorized. Split it into smaller methods.}}
    // 2
    // 3
    // 4
  } // 5
}
