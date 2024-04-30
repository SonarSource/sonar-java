class A {
  {
    return; // Compliant
    return;
  }

  {
    return; // Compliant
    return;
    return;
    return;
  }

  void foo() { // Noncompliant {{This method has 4 returns, which is more than the 3 allowed.}}
//     ^^^
    return;
    return;
    return;
    return;
  }

  boolean foo2() { // Noncompliant {{This method has 4 returns, which is more than the 3 allowed.}}
//        ^^^^
    return true;
//  ^^^^^^<
    return false;
//  ^^^^^^<
    return true;
//  ^^^^^^<
    return false;
//  ^^^^^^<
  }
}
