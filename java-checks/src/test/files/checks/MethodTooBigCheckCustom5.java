class A {
  public A() { // Noncompliant {{This method has 6 lines, which is greater than the 5 lines authorized. Split it into smaller methods.}}
    // 2
    // 3
    // 4
    // 5
  } // 6

  public void f() { // 1
    // 2
    // 3
    // 4
  } // 5
}
