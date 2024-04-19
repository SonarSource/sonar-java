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

  void foo() { // Noncompliant [[sc=8;ec=11]] {{This method has 4 returns, which is more than the 3 allowed.}}
    return;
    return;
    return;
    return;
  }

  boolean foo2() { // Noncompliant [[sc=11;ec=15;secondary=22,23,24,25]] {{This method has 4 returns, which is more than the 3 allowed.}}
    return true;
    return false;
    return true;
    return false;
  }
}
